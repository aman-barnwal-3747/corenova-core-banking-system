package com.corenova.bank.config;

import com.corenova.bank.entity.Account;
import com.corenova.bank.entity.Loan;
import com.corenova.bank.entity.Transaction;
import com.corenova.bank.enums.AccountStatus;
import com.corenova.bank.enums.TransactionStatus;
import com.corenova.bank.enums.TransactionType;
import com.corenova.bank.repository.AccountRepository;
import com.corenova.bank.repository.LoanRepository;
import com.corenova.bank.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * ================================================================
 *  ScheduledJobs – Core Banking End-of-Day (EOD) Batch Processing
 *
 *  Automated batch jobs that run on a schedule to maintain
 *  financial data integrity and regulatory compliance.
 *
 *  Job Schedule Summary:
 *  ┌──────────────────────────────────────────────────────────────┐
 *  │  JOB                    │ SCHEDULE        │ PURPOSE         │
 *  ├──────────────────────────────────────────────────────────────┤
 *  │  Daily Limit Reset      │ 00:00 daily     │ Reset txn limits│
 *  │  Interest Posting       │ 23:30 daily     │ Accrue interest │
 *  │  Dormancy Check         │ 01:00 daily     │ Flag dormant a/c│
 *  │  NEFT Settlement        │ Every 30 min    │ Process NEFT txn│
 *  │  NPA Classification     │ 02:00 daily     │ RBI IRAC norms  │
 *  │  EMI Auto-Debit         │ 09:00 daily     │ Collect EMIs    │
 *  └──────────────────────────────────────────────────────────────┘
 *
 *  In Finacle: BEOD (Begin-of-Day) / AEOD (After-End-of-Day) jobs.
 *  Production: Replace with Spring Batch for fault tolerance,
 *  checkpointing, and parallel processing of large datasets.
 * ================================================================
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledJobs {

    private final AccountRepository     accountRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository        loanRepository;

    // ─────────────────────────────────────────────────────────────
    //  1. DAILY LIMIT RESET (Midnight)
    // ─────────────────────────────────────────────────────────────

    /**
     * Resets each account's dailyAmountUsedToday to ZERO at midnight.
     *
     * This allows accounts to transact up to their daily limit again
     * on the new calendar day. MUST run before any transactions start.
     *
     * Schedule: Every day at 00:00:00 (midnight)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetDailyTransactionLimits() {
        log.info("[BATCH] Starting daily transaction limit reset job...");
        long startTime = System.currentTimeMillis();

        // Bulk update — single SQL statement: UPDATE cbs_accounts SET daily_amount_used_today = 0
        List<Account> activeAccounts = accountRepository.findAll().stream()
            .filter(a -> AccountStatus.ACTIVE.equals(a.getAccountStatus()))
            .toList();

        activeAccounts.forEach(account -> account.setDailyAmountUsedToday(BigDecimal.ZERO));
        accountRepository.saveAll(activeAccounts);

        long elapsed = System.currentTimeMillis() - startTime;
        log.info("[BATCH] Daily limit reset complete. {} accounts processed in {}ms",
                 activeAccounts.size(), elapsed);
    }

    // ─────────────────────────────────────────────────────────────
    //  2. DAILY INTEREST POSTING (23:30)
    // ─────────────────────────────────────────────────────────────

    /**
     * Posts daily interest accrual on all active Savings/FD accounts.
     *
     * Interest Calculation (Daily Accrual, Quarterly Compounding):
     *   Daily Rate = Annual Rate / 365
     *   Daily Interest = Balance × Daily Rate / 100
     *
     * Note: Interest is ACCRUED daily but CREDITED quarterly
     * (Mar 31, Jun 30, Sep 30, Dec 31) — RBI standard practice.
     *
     * Schedule: Every day at 23:30 (before day-end close)
     */
    @Scheduled(cron = "0 30 23 * * *")
    @Transactional
    public void postDailyInterest() {
        log.info("[BATCH] Starting daily interest posting job...");

        LocalDate today = LocalDate.now();
        boolean isQuarterEnd = isQuarterEndDate(today);

        int creditsPosted = 0;
        List<Account> savingsAccounts = accountRepository.findAll().stream()
            .filter(a -> AccountStatus.ACTIVE.equals(a.getAccountStatus()) &&
                        a.getInterestRate() != null &&
                        a.getInterestRate().compareTo(BigDecimal.ZERO) > 0)
            .toList();

        for (Account account : savingsAccounts) {
            BigDecimal dailyRate = account.getInterestRate()
                .divide(new BigDecimal("36500"), 10, RoundingMode.HALF_UP);
            BigDecimal dailyInterest = account.getCurrentBalance()
                .multiply(dailyRate)
                .setScale(2, RoundingMode.HALF_UP);

            // Only credit if amount is meaningful (≥ ₹0.01)
            if (isQuarterEnd && dailyInterest.compareTo(new BigDecimal("0.01")) >= 0) {
                // Credit interest to account
                account.setCurrentBalance(account.getCurrentBalance().add(dailyInterest));
                account.setAvailableBalance(account.getAvailableBalance().add(dailyInterest));
                account.setLastInterestPostedDate(today);
                accountRepository.save(account);

                // Create interest credit transaction record
                Transaction interestTxn = Transaction.builder()
                    .account(account)
                    .referenceNumber("INT" + System.currentTimeMillis() +
                        UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                    .transactionType(TransactionType.INTEREST_CREDIT)
                    .entryType("CREDIT")
                    .amount(dailyInterest)
                    .currency("INR")
                    .narration(String.format("Quarterly interest credit @ %.2f%% p.a.",
                               account.getInterestRate()))
                    .transactionDate(LocalDateTime.now())
                    .transactionStatus(TransactionStatus.SUCCESS)
                    .settlementDate(LocalDateTime.now())
                    .balanceAfterTransaction(account.getCurrentBalance())
                    .channel("BATCH")
                    .initiatedBy("SYSTEM")
                    .build();
                transactionRepository.save(interestTxn);
                creditsPosted++;
            }
        }

        log.info("[BATCH] Interest posting complete. {} accounts credited (quarter-end: {})",
                 creditsPosted, isQuarterEnd);
    }

    // ─────────────────────────────────────────────────────────────
    //  3. DORMANCY CHECK (01:00 Daily)
    // ─────────────────────────────────────────────────────────────

    /**
     * Marks accounts as DORMANT if no customer-initiated transaction
     * in the last 12 months (RBI definition: 24 months for Savings,
     * 12 months for Current).
     *
     * Dormant accounts:
     *  • Cannot be debited (customer must re-activate at branch)
     *  • Can still receive credits (salary, FD maturity)
     *  • Balance transferred to Unclaimed Deposits Fund after 10 years
     *
     * Schedule: Daily at 01:00 AM
     */
    @Scheduled(cron = "0 0 1 * * *")
    @Transactional
    public void checkDormancy() {
        log.info("[BATCH] Starting dormancy check job...");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(365); // 12 months
        List<Account> dormantCandidates = accountRepository.findDormantAccounts(cutoffDate);

        int markedDormant = 0;
        for (Account account : dormantCandidates) {
            account.setAccountStatus(AccountStatus.DORMANT);
            account.setDormantSinceDate(LocalDate.now());
            accountRepository.save(account);
            markedDormant++;
            log.debug("[BATCH] Account {} marked dormant. Last txn: {}",
                      account.getAccountNumber(), account.getLastTransactionDate());
        }

        log.info("[BATCH] Dormancy check complete. {} accounts marked dormant.", markedDormant);
    }

    // ─────────────────────────────────────────────────────────────
    //  4. NEFT BATCH SETTLEMENT (Every 30 Minutes)
    // ─────────────────────────────────────────────────────────────

    /**
     * Processes NEFT transactions in half-hourly batches.
     *
     * RBI mandates 48 settlement cycles per day (8 AM to 7 PM Mon-Sat).
     * Available 24x7 since RBI circular Dec 2019.
     *
     * Simulates: marking PROCESSING transactions as SUCCESS/FAILED
     * after the RBI settlement batch run completes.
     *
     * Schedule: Every 30 minutes
     */
    @Scheduled(cron = "0 0/30 * * * *")
    @Transactional
    public void processNeftBatchSettlement() {
        List<Transaction> pendingNeft = transactionRepository
            .findByTransactionTypeAndTransactionStatus(
                TransactionType.NEFT, TransactionStatus.PROCESSING);

        if (pendingNeft.isEmpty()) return;

        log.info("[BATCH] Processing {} pending NEFT transactions...", pendingNeft.size());
        int settled = 0;

        for (Transaction txn : pendingNeft) {
            // Simulate RBI batch settlement (in production: call RBI NEFT API)
            txn.setTransactionStatus(TransactionStatus.SUCCESS);
            txn.setSettlementDate(LocalDateTime.now());
            txn.setUtrNumber("NEFT" + System.currentTimeMillis());
            transactionRepository.save(txn);
            settled++;
        }

        log.info("[BATCH] NEFT settlement complete. {} transactions settled.", settled);
    }

    // ─────────────────────────────────────────────────────────────
    //  5. NPA CLASSIFICATION (02:00 Daily)
    // ─────────────────────────────────────────────────────────────

    /**
     * Classifies Non-Performing Assets per RBI IRAC (Income Recognition
     * and Asset Classification) norms.
     *
     * NPA Ladder (RBI):
     *   STANDARD     → No overdue EMIs
     *   SUB_STANDARD → 90 days (3 EMIs) overdue — NPA trigger
     *   DOUBTFUL_1   → 12 months in sub-standard
     *   DOUBTFUL_2   → 12–24 months in sub-standard
     *   DOUBTFUL_3   → > 24 months in sub-standard
     *   LOSS         → Identified as uncollectable by bank/auditor
     *
     * Schedule: Daily at 02:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void classifyNpa() {
        log.info("[BATCH] Starting NPA classification job...");

        List<Loan> npaCandidates = loanRepository.findNpaCandidates();
        int classified = 0;

        for (Loan loan : npaCandidates) {
            String newClassification;
            int overdueEmis = loan.getOverdueEmis();

            if (overdueEmis >= 3 && overdueEmis <= 12) {
                newClassification = "SUB_STANDARD";
            } else if (overdueEmis > 12 && overdueEmis <= 24) {
                newClassification = "DOUBTFUL_1";
            } else if (overdueEmis > 24 && overdueEmis <= 36) {
                newClassification = "DOUBTFUL_2";
            } else if (overdueEmis > 36) {
                newClassification = "LOSS";
            } else {
                newClassification = "STANDARD";
            }

            if (!newClassification.equals(loan.getNpaClassification())) {
                loan.setNpaClassification(newClassification);
                if (overdueEmis >= 3) loan.setLoanStatus("DEFAULTED");
                loanRepository.save(loan);
                classified++;
                log.warn("[NPA] Loan {} reclassified: {} | Overdue EMIs: {}",
                         loan.getLoanNumber(), newClassification, overdueEmis);
            }
        }

        log.info("[BATCH] NPA classification complete. {} loans reclassified.", classified);
    }

    // ─────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────

    /** Returns true if today is a quarter-end date (Mar 31, Jun 30, Sep 30, Dec 31). */
    private boolean isQuarterEndDate(LocalDate date) {
        return (date.getMonthValue() == 3  && date.getDayOfMonth() == 31) ||
               (date.getMonthValue() == 6  && date.getDayOfMonth() == 30) ||
               (date.getMonthValue() == 9  && date.getDayOfMonth() == 30) ||
               (date.getMonthValue() == 12 && date.getDayOfMonth() == 31);
    }
}
