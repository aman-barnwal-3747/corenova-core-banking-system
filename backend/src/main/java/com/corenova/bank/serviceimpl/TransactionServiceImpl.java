package com.corenova.bank.serviceimpl;

import com.corenova.bank.audit.AuditLogService;
import com.corenova.bank.dto.request.FundTransferRequest;
import com.corenova.bank.dto.request.TransactionRequest;
import com.corenova.bank.dto.response.TransactionResponse;
import com.corenova.bank.entity.Account;
import com.corenova.bank.entity.Transaction;
import com.corenova.bank.enums.AccountStatus;
import com.corenova.bank.enums.TransactionStatus;
import com.corenova.bank.enums.TransactionType;
import com.corenova.bank.exception.BankingException;
import com.corenova.bank.repository.AccountRepository;
import com.corenova.bank.repository.TransactionRepository;
import com.corenova.bank.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ================================================================
 *  TransactionServiceImpl – Core Banking Transaction Engine
 *
 *  This is the most critical class in the CoreNova Bank system.
 *  All money movement flows through this service.
 *
 *  Architecture mirrors Finacle's Transaction Processing System (TPS):
 *  ┌────────────────────────────────────────────────────────────┐
 *  │  1. Validate JWT / Authorization                          │
 *  │  2. Validate source & destination accounts               │
 *  │  3. Verify daily limits & regulatory thresholds          │
 *  │  4. Fraud detection check (placeholder for ML model)     │
 *  │  5. Check for maker-checker approval requirement         │
 *  │  6. ACQUIRE PESSIMISTIC LOCK on source account           │
 *  │  7. Debit source account (within @Transactional)         │
 *  │  8. Credit destination account                           │
 *  │  9. Create Transaction records (debit + credit)          │
 *  │  10. Update daily amount used                            │
 *  │  11. Generate UTR / reference number                     │
 *  │  12. Write audit log (async)                             │
 *  │  13. Dispatch SMS/email notification (async)             │
 *  │  14. Commit — return TransactionResponse                 │
 *  └────────────────────────────────────────────────────────────┘
 *
 *  @Transactional(isolation = REPEATABLE_READ) prevents:
 *    • Dirty reads : Never read uncommitted data
 *    • Non-repeatable reads : Balance doesn't change mid-transaction
 *    • Phantom rows : No new rows appear during transaction
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final AccountRepository     accountRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogService       auditLogService;

    /** Thread-safe sequence counter for same-second transaction ordering. */
    private final AtomicLong txnSequence = new AtomicLong(0);

    // ── Regulatory Limits (configurable via @Value in production) ──
    private static final BigDecimal RTGS_MINIMUM          = new BigDecimal("200000.00");
    private static final BigDecimal UPI_PER_TXN_LIMIT     = new BigDecimal("100000.00");
    private static final BigDecimal IMPS_LIMIT            = new BigDecimal("500000.00");
    private static final BigDecimal HIGH_VALUE_THRESHOLD  = new BigDecimal("500000.00");

    // ─────────────────────────────────────────────────────────────────
    //  INTERNAL FUND TRANSFER
    // ─────────────────────────────────────────────────────────────────

    /**
     * Executes an ACID-compliant fund transfer between two CoreNova accounts.
     *
     * Uses REPEATABLE_READ isolation + pessimistic locking to prevent
     * double-spend in concurrent transfer scenarios (e.g. same account
     * initiating two transfers simultaneously from different sessions).
     *
     * @param request  Contains fromAccount, toAccount, amount, remarks
     * @return         Transaction confirmation with reference number
     */
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public TransactionResponse initiateInternalTransfer(FundTransferRequest request) {

        log.info("Initiating internal transfer: ₹{} from [{}] to [{}]",
                 request.getAmount(), request.getFromAccountNumber(), request.getToAccountNumber());

        // ── Step 1: Load accounts with PESSIMISTIC WRITE lock ────────
        // LockModeType.PESSIMISTIC_WRITE acquires a DB-level row lock,
        // preventing concurrent updates until this transaction commits.
        Account sourceAccount = accountRepository
            .findByAccountNumberWithLock(request.getFromAccountNumber())
            .orElseThrow(() -> new BankingException("Source account not found: " + request.getFromAccountNumber()));

        Account destAccount = accountRepository
            .findByAccountNumber(request.getToAccountNumber())
            .orElseThrow(() -> new BankingException("Destination account not found: " + request.getToAccountNumber()));

        // ── Step 2: Validate both accounts ──────────────────────────
        validateAccountForDebit(sourceAccount, request.getAmount());
        validateAccountForCredit(destAccount);

        // ── Step 3: Check daily transfer limit ──────────────────────
        validateDailyLimit(sourceAccount, request.getAmount());

        // ── Step 4: Generate unique reference number ─────────────────
        String referenceNumber = generateReferenceNumber("TXN");

        // ── Step 5: Debit the source account ─────────────────────────
        sourceAccount.setCurrentBalance(
            sourceAccount.getCurrentBalance().subtract(request.getAmount())
        );
        sourceAccount.setAvailableBalance(
            sourceAccount.getAvailableBalance().subtract(request.getAmount())
        );
        sourceAccount.setDailyAmountUsedToday(
            sourceAccount.getDailyAmountUsedToday().add(request.getAmount())
        );
        sourceAccount.setLastTransactionDate(LocalDateTime.now());
        accountRepository.save(sourceAccount);

        // ── Step 6: Credit the destination account ───────────────────
        destAccount.setCurrentBalance(
            destAccount.getCurrentBalance().add(request.getAmount())
        );
        destAccount.setAvailableBalance(
            destAccount.getAvailableBalance().add(request.getAmount())
        );
        destAccount.setLastTransactionDate(LocalDateTime.now());
        accountRepository.save(destAccount);

        // ── Step 7: Create DEBIT transaction record ───────────────────
        Transaction debitTxn = buildTransaction(
            sourceAccount, request.getAmount(), "DEBIT",
            TransactionType.INTERNAL_TRANSFER, referenceNumber,
            "Transfer to " + destAccount.getAccountHolderName()
              + " (" + request.getToAccountNumber() + ")",
            sourceAccount.getCurrentBalance(),
            request.getRemarks()
        );
        debitTxn.setCounterpartyAccountNumber(request.getToAccountNumber());
        debitTxn.setCounterpartyName(destAccount.getAccountHolderName());
        debitTxn.setTransactionStatus(TransactionStatus.SUCCESS);
        debitTxn.setSettlementDate(LocalDateTime.now());

        // ── Step 8: Create CREDIT transaction record ──────────────────
        Transaction creditTxn = buildTransaction(
            destAccount, request.getAmount(), "CREDIT",
            TransactionType.INTERNAL_TRANSFER, referenceNumber,
            "Transfer from " + sourceAccount.getAccountHolderName()
              + " (" + request.getFromAccountNumber() + ")",
            destAccount.getCurrentBalance(),
            request.getRemarks()
        );
        creditTxn.setCounterpartyAccountNumber(request.getFromAccountNumber());
        creditTxn.setCounterpartyName(sourceAccount.getAccountHolderName());
        creditTxn.setTransactionStatus(TransactionStatus.SUCCESS);
        creditTxn.setSettlementDate(LocalDateTime.now());

        // ── Step 9: Persist both transaction records ──────────────────
        transactionRepository.save(debitTxn);
        transactionRepository.save(creditTxn);

        // ── Step 10: Write audit log (async — non-blocking) ───────────
        String initiatedBy = getCurrentUsername();
        auditLogService.logAsync(
            initiatedBy, "FUND_TRANSFER", "ACCOUNT",
            request.getFromAccountNumber(),
            String.format("Internal transfer ₹%s from %s to %s",
                request.getAmount(), request.getFromAccountNumber(), request.getToAccountNumber()),
            "SUCCESS", null
        );

        log.info("Transfer SUCCESS: ref={}, ₹{} from {} to {}",
                 referenceNumber, request.getAmount(),
                 request.getFromAccountNumber(), request.getToAccountNumber());

        // ── Step 11: Build and return response ────────────────────────
        return TransactionResponse.builder()
            .referenceNumber(referenceNumber)
            .transactionType(TransactionType.INTERNAL_TRANSFER.name())
            .amount(request.getAmount())
            .status(TransactionStatus.SUCCESS.name())
            .fromAccountNumber(request.getFromAccountNumber())
            .toAccountNumber(request.getToAccountNumber())
            .narration(debitTxn.getNarration())
            .transactionDate(debitTxn.getTransactionDate())
            .balanceAfter(sourceAccount.getCurrentBalance())
            .message("Fund transfer successful")
            .build();
    }

    // ─────────────────────────────────────────────────────────────────
    //  UPI PAYMENT
    // ─────────────────────────────────────────────────────────────────

    /**
     * Processes a UPI payment (Unified Payments Interface).
     *
     * UPI Flow simulation:
     *   Customer → Enter UPI ID → NPCI resolve VPA → Validate payee →
     *   Authenticate (MPIN) → Debit sender → Credit receiver → Send notification
     *
     * Limits (NPCI mandated):
     *   • ₹1,00,000 per transaction
     *   • ₹1,00,000 per day (can vary by bank)
     *
     * @param request  Contains fromAccount, upiId (VPA), amount
     */
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public TransactionResponse processUpiPayment(TransactionRequest request) {

        // Validate UPI-specific limit
        if (request.getAmount().compareTo(UPI_PER_TXN_LIMIT) > 0) {
            throw new BankingException(
                "UPI transaction limit is ₹1,00,000 per transaction. " +
                "Use IMPS or NEFT for higher amounts."
            );
        }

        Account sourceAccount = accountRepository
            .findByAccountNumberWithLock(request.getAccountNumber())
            .orElseThrow(() -> new BankingException("Account not found: " + request.getAccountNumber()));

        validateAccountForDebit(sourceAccount, request.getAmount());
        validateDailyLimit(sourceAccount, request.getAmount());

        // In production: call NPCI UPI API to resolve VPA and credit beneficiary
        // Here we simulate with a successful response
        String referenceNumber = generateReferenceNumber("UPI");
        String utrNumber       = "UPI" + System.currentTimeMillis();

        // Debit source account
        sourceAccount.setCurrentBalance(sourceAccount.getCurrentBalance().subtract(request.getAmount()));
        sourceAccount.setAvailableBalance(sourceAccount.getAvailableBalance().subtract(request.getAmount()));
        sourceAccount.setDailyAmountUsedToday(sourceAccount.getDailyAmountUsedToday().add(request.getAmount()));
        accountRepository.save(sourceAccount);

        // Create transaction record
        Transaction txn = buildTransaction(
            sourceAccount, request.getAmount(), "DEBIT",
            TransactionType.UPI, referenceNumber,
            "UPI payment to " + request.getCounterpartyUpiId(),
            sourceAccount.getCurrentBalance(), request.getRemarks()
        );
        txn.setUtrNumber(utrNumber);
        txn.setCounterpartyUpiId(request.getCounterpartyUpiId());
        txn.setTransactionStatus(TransactionStatus.SUCCESS);
        txn.setSettlementDate(LocalDateTime.now());
        transactionRepository.save(txn);

        auditLogService.logAsync(getCurrentUsername(), "UPI_PAYMENT", "ACCOUNT",
            request.getAccountNumber(),
            String.format("UPI ₹%s to %s", request.getAmount(), request.getCounterpartyUpiId()),
            "SUCCESS", null);

        return TransactionResponse.builder()
            .referenceNumber(referenceNumber)
            .utrNumber(utrNumber)
            .transactionType(TransactionType.UPI.name())
            .amount(request.getAmount())
            .status(TransactionStatus.SUCCESS.name())
            .fromAccountNumber(request.getAccountNumber())
            .narration(txn.getNarration())
            .transactionDate(txn.getTransactionDate())
            .balanceAfter(sourceAccount.getCurrentBalance())
            .message("UPI payment successful")
            .build();
    }

    // ─────────────────────────────────────────────────────────────────
    //  NEFT TRANSFER
    // ─────────────────────────────────────────────────────────────────

    /**
     * Initiates an NEFT (National Electronic Funds Transfer) to another bank.
     *
     * NEFT characteristics:
     *   • Batch-based: Settled in 48 half-hourly batches (8 AM – 7 PM, Mon-Sat)
     *   • No minimum amount
     *   • Available 24x7 (RBI mandate since Dec 2019)
     *   • Transaction tagged PROCESSING until batch settlement
     */
    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public TransactionResponse processNeftTransfer(TransactionRequest request) {

        Account sourceAccount = accountRepository
            .findByAccountNumberWithLock(request.getAccountNumber())
            .orElseThrow(() -> new BankingException("Account not found"));

        validateAccountForDebit(sourceAccount, request.getAmount());
        validateDailyLimit(sourceAccount, request.getAmount());
        validateIfscCode(request.getCounterpartyIfsc());

        String referenceNumber = generateReferenceNumber("NEFT");
        String utrNumber       = "NEFT" + System.currentTimeMillis();

        // Debit sender immediately; credit side is batch-settled
        sourceAccount.setCurrentBalance(sourceAccount.getCurrentBalance().subtract(request.getAmount()));
        sourceAccount.setAvailableBalance(sourceAccount.getAvailableBalance().subtract(request.getAmount()));
        accountRepository.save(sourceAccount);

        Transaction txn = buildTransaction(
            sourceAccount, request.getAmount(), "DEBIT",
            TransactionType.NEFT, referenceNumber,
            String.format("NEFT to %s/%s via %s",
                request.getCounterpartyName(), request.getCounterpartyAccountNumber(), request.getCounterpartyIfsc()),
            sourceAccount.getCurrentBalance(), request.getRemarks()
        );
        txn.setUtrNumber(utrNumber);
        txn.setCounterpartyAccountNumber(request.getCounterpartyAccountNumber());
        txn.setCounterpartyName(request.getCounterpartyName());
        txn.setCounterpartyBankName(request.getCounterpartyBankName());
        txn.setCounterpartyIfsc(request.getCounterpartyIfsc());
        // NEFT is batch-processed — status stays PROCESSING until settlement
        txn.setTransactionStatus(TransactionStatus.PROCESSING);
        transactionRepository.save(txn);

        auditLogService.logAsync(getCurrentUsername(), "NEFT_TRANSFER", "ACCOUNT",
            request.getAccountNumber(),
            String.format("NEFT ₹%s to %s (%s)", request.getAmount(),
                request.getCounterpartyName(), request.getCounterpartyIfsc()),
            "SUCCESS", null);

        return TransactionResponse.builder()
            .referenceNumber(referenceNumber)
            .utrNumber(utrNumber)
            .transactionType(TransactionType.NEFT.name())
            .amount(request.getAmount())
            .status(TransactionStatus.PROCESSING.name())
            .fromAccountNumber(request.getAccountNumber())
            .transactionDate(txn.getTransactionDate())
            .balanceAfter(sourceAccount.getCurrentBalance())
            .message("NEFT initiated successfully. Settlement within 2 hours.")
            .build();
    }

    // ─────────────────────────────────────────────────────────────────
    //  RTGS TRANSFER
    // ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public TransactionResponse processRtgsTransfer(TransactionRequest request) {

        if (request.getAmount().compareTo(RTGS_MINIMUM) < 0) {
            throw new BankingException("RTGS minimum amount is ₹2,00,000. Use NEFT or IMPS for smaller amounts.");
        }

        Account sourceAccount = accountRepository
            .findByAccountNumberWithLock(request.getAccountNumber())
            .orElseThrow(() -> new BankingException("Account not found"));

        validateAccountForDebit(sourceAccount, request.getAmount());

        String referenceNumber = generateReferenceNumber("RTGS");
        String utrNumber       = "RTGS" + System.currentTimeMillis();

        sourceAccount.setCurrentBalance(sourceAccount.getCurrentBalance().subtract(request.getAmount()));
        sourceAccount.setAvailableBalance(sourceAccount.getAvailableBalance().subtract(request.getAmount()));
        accountRepository.save(sourceAccount);

        Transaction txn = buildTransaction(
            sourceAccount, request.getAmount(), "DEBIT",
            TransactionType.RTGS, referenceNumber,
            String.format("RTGS to %s/%s via %s",
                request.getCounterpartyName(), request.getCounterpartyAccountNumber(), request.getCounterpartyIfsc()),
            sourceAccount.getCurrentBalance(), request.getRemarks()
        );
        txn.setUtrNumber(utrNumber);
        txn.setCounterpartyAccountNumber(request.getCounterpartyAccountNumber());
        txn.setCounterpartyName(request.getCounterpartyName());
        txn.setCounterpartyIfsc(request.getCounterpartyIfsc());
        txn.setTransactionStatus(TransactionStatus.SUCCESS); // RTGS = real-time settlement
        txn.setSettlementDate(LocalDateTime.now());
        transactionRepository.save(txn);

        return TransactionResponse.builder()
            .referenceNumber(referenceNumber)
            .utrNumber(utrNumber)
            .transactionType(TransactionType.RTGS.name())
            .amount(request.getAmount())
            .status(TransactionStatus.SUCCESS.name())
            .fromAccountNumber(request.getAccountNumber())
            .transactionDate(txn.getTransactionDate())
            .balanceAfter(sourceAccount.getCurrentBalance())
            .message("RTGS transfer completed successfully.")
            .build();
    }

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public TransactionResponse processImpsTransfer(TransactionRequest request) {
        // IMPS = 24x7 immediate — same logic as RTGS but lower limit
        if (request.getAmount().compareTo(IMPS_LIMIT) > 0) {
            throw new BankingException("IMPS limit is ₹5,00,000 per transaction.");
        }
        Account sourceAccount = accountRepository
            .findByAccountNumberWithLock(request.getAccountNumber())
            .orElseThrow(() -> new BankingException("Account not found"));
        validateAccountForDebit(sourceAccount, request.getAmount());
        validateDailyLimit(sourceAccount, request.getAmount());

        String referenceNumber = generateReferenceNumber("IMPS");
        sourceAccount.setCurrentBalance(sourceAccount.getCurrentBalance().subtract(request.getAmount()));
        sourceAccount.setAvailableBalance(sourceAccount.getAvailableBalance().subtract(request.getAmount()));
        accountRepository.save(sourceAccount);

        Transaction txn = buildTransaction(
            sourceAccount, request.getAmount(), "DEBIT",
            TransactionType.IMPS, referenceNumber,
            "IMPS to " + request.getCounterpartyName(),
            sourceAccount.getCurrentBalance(), request.getRemarks()
        );
        txn.setTransactionStatus(TransactionStatus.SUCCESS);
        txn.setSettlementDate(LocalDateTime.now());
        transactionRepository.save(txn);

        return TransactionResponse.builder()
            .referenceNumber(referenceNumber)
            .transactionType(TransactionType.IMPS.name())
            .amount(request.getAmount())
            .status(TransactionStatus.SUCCESS.name())
            .balanceAfter(sourceAccount.getCurrentBalance())
            .message("IMPS transfer successful.")
            .build();
    }

    // ─────────────────────────────────────────────────────────────────
    //  QUERY OPERATIONS
    // ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(String accountNumber, Pageable pageable) {
        return transactionRepository
            .findByAccount_AccountNumberOrderByTransactionDateDesc(accountNumber, pageable)
            .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getByReferenceNumber(String referenceNumber) {
        Transaction txn = transactionRepository
            .findByReferenceNumber(referenceNumber)
            .orElseThrow(() -> new BankingException("Transaction not found: " + referenceNumber));
        return mapToResponse(txn);
    }

    // ─────────────────────────────────────────────────────────────────
    //  REVERSAL
    // ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(isolation = Isolation.REPEATABLE_READ, rollbackFor = Exception.class)
    public TransactionResponse reverseTransaction(String referenceNumber, String remarks) {
        Transaction original = transactionRepository
            .findByReferenceNumber(referenceNumber)
            .orElseThrow(() -> new BankingException("Transaction not found: " + referenceNumber));

        if (!TransactionStatus.SUCCESS.equals(original.getTransactionStatus())) {
            throw new BankingException("Only successful transactions can be reversed.");
        }

        Account account = original.getAccount();

        // Reverse: DEBIT becomes CREDIT and vice versa
        String reversalEntryType = "DEBIT".equals(original.getEntryType()) ? "CREDIT" : "DEBIT";
        if ("CREDIT".equals(reversalEntryType)) {
            account.setCurrentBalance(account.getCurrentBalance().add(original.getAmount()));
            account.setAvailableBalance(account.getAvailableBalance().add(original.getAmount()));
        } else {
            validateAccountForDebit(account, original.getAmount());
            account.setCurrentBalance(account.getCurrentBalance().subtract(original.getAmount()));
            account.setAvailableBalance(account.getAvailableBalance().subtract(original.getAmount()));
        }
        accountRepository.save(account);

        String reversalRef = generateReferenceNumber("REV");
        Transaction reversal = buildTransaction(
            account, original.getAmount(), reversalEntryType,
            TransactionType.REVERSAL, reversalRef,
            "REVERSAL of " + referenceNumber + ". " + remarks,
            account.getCurrentBalance(), remarks
        );
        reversal.setOriginalTransactionRef(referenceNumber);
        reversal.setTransactionStatus(TransactionStatus.REVERSED);
        transactionRepository.save(reversal);

        original.setTransactionStatus(TransactionStatus.REVERSED);
        transactionRepository.save(original);

        return mapToResponse(reversal);
    }

    // ─────────────────────────────────────────────────────────────────
    //  VALIDATION HELPERS
    // ─────────────────────────────────────────────────────────────────

    private void validateAccountForDebit(Account account, BigDecimal amount) {
        if (!AccountStatus.ACTIVE.equals(account.getAccountStatus())) {
            throw new BankingException(
                "Account " + account.getAccountNumber() + " is not active. Status: " + account.getAccountStatus()
            );
        }
        if (!account.hasSufficientBalance(amount)) {
            throw new BankingException(
                String.format("Insufficient balance. Available: ₹%s, Requested: ₹%s",
                    account.getAvailableBalance(), amount)
            );
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Transfer amount must be greater than zero.");
        }
    }

    private void validateAccountForCredit(Account account) {
        if (AccountStatus.CLOSED.equals(account.getAccountStatus()) ||
            AccountStatus.FROZEN.equals(account.getAccountStatus())) {
            throw new BankingException(
                "Destination account " + account.getAccountNumber() + " cannot receive funds. Status: " + account.getAccountStatus()
            );
        }
    }

    private void validateDailyLimit(Account account, BigDecimal amount) {
        BigDecimal projectedDailyTotal = account.getDailyAmountUsedToday().add(amount);
        if (projectedDailyTotal.compareTo(account.getDailyTransactionLimit()) > 0) {
            throw new BankingException(
                String.format("Daily transaction limit of ₹%s exceeded. Used today: ₹%s",
                    account.getDailyTransactionLimit(), account.getDailyAmountUsedToday())
            );
        }
    }

    private void validateIfscCode(String ifsc) {
        // IFSC format: 4 alpha (bank code) + 0 + 6 alphanumeric (branch code)
        if (ifsc == null || !ifsc.matches("^[A-Z]{4}0[A-Z0-9]{6}$")) {
            throw new BankingException("Invalid IFSC code format: " + ifsc);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  BUILDER HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Factory method — builds a Transaction entity with common fields.
     * Reduces duplication across all payment methods.
     */
    private Transaction buildTransaction(
            Account account, BigDecimal amount, String entryType,
            TransactionType type, String referenceNumber,
            String narration, BigDecimal balanceAfter, String remarks) {

        return Transaction.builder()
            .account(account)
            .referenceNumber(referenceNumber)
            .transactionType(type)
            .entryType(entryType)
            .amount(amount)
            .currency(account.getCurrency())
            .narration(narration)
            .remarks(remarks)
            .balanceAfterTransaction(balanceAfter)
            .transactionDate(LocalDateTime.now())
            .initiatedBy(getCurrentUsername())
            .channel("CBS_COUNTER")
            .transactionStatus(TransactionStatus.INITIATED)
            .build();
    }

    /**
     * Generates a unique transaction reference number.
     * Format: {PREFIX}{yyyyMMddHHmmss}{UUID-8chars}
     * Example: TXN20250520103045a1b2c3d4
     */
    private String generateReferenceNumber(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String unique    = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return prefix + timestamp + unique;
    }

    /** Gets the username of the currently authenticated user from Spring Security. */
    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }

    /** Maps Transaction entity to response DTO. */
    private TransactionResponse mapToResponse(Transaction txn) {
        return TransactionResponse.builder()
            .transactionId(txn.getTransactionId())
            .referenceNumber(txn.getReferenceNumber())
            .utrNumber(txn.getUtrNumber())
            .transactionType(txn.getTransactionType().name())
            .entryType(txn.getEntryType())
            .amount(txn.getAmount())
            .currency(txn.getCurrency())
            .status(txn.getTransactionStatus().name())
            .narration(txn.getNarration())
            .counterpartyName(txn.getCounterpartyName())
            .counterpartyAccountNumber(txn.getCounterpartyAccountNumber())
            .transactionDate(txn.getTransactionDate())
            .settlementDate(txn.getSettlementDate())
            .balanceAfter(txn.getBalanceAfterTransaction())
            .build();
    }
}
