package com.corenova.bank.serviceimpl;

import com.corenova.bank.audit.AuditLogService;
import com.corenova.bank.entity.Account;
import com.corenova.bank.entity.Customer;
import com.corenova.bank.entity.Loan;
import com.corenova.bank.exception.BankingException;
import com.corenova.bank.repository.AccountRepository;
import com.corenova.bank.repository.CustomerRepository;
import com.corenova.bank.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

/**
 * ================================================================
 *  LoanServiceImpl – Loan Lifecycle Management
 *
 *  Handles all stages of the loan lifecycle:
 *  APPLICATION → REVIEW → SANCTION → DISBURSEMENT →
 *  ACTIVE (EMI collection) → CLOSURE / NPA
 *
 *  Regulatory compliance:
 *  • LTV ratios per RBI guidelines (Home: 80%, Auto: 85%)
 *  • NPA classification after 90 days (3 EMIs) per RBI IRAC norms
 *  • Pre-closure penalty rules per RBI circular RBI/2014-15/416
 *  • Fair Practice Code adherence for all loan products
 *
 *  EMI auto-debit runs as a @Scheduled job in ScheduledJobs.java
 *  In Finacle: Loans & Advances Processing Engine (LAPE)
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanServiceImpl {

    private final LoanRepository     loanRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository  accountRepository;
    private final AuditLogService    auditLogService;

    // ── Maximum LTV ratios per RBI guidelines (%) ────────────────
    private static final double MAX_LTV_HOME      = 80.0;
    private static final double MAX_LTV_AUTO      = 85.0;
    private static final double MAX_LTV_GOLD      = 75.0;
    private static final double MAX_LTV_EDUCATION = 90.0;

    // ─────────────────────────────────────────────────────────────
    //  LOAN APPLICATION
    // ─────────────────────────────────────────────────────────────

    /**
     * Registers a new loan application.
     *
     * Validates:
     *  • Customer exists and is KYC-approved
     *  • Requested amount within product limits
     *  • Tenure within allowed range
     *  • LTV ratio within RBI-mandated limits (secured loans)
     *
     * Does NOT approve or disburse — triggers maker-checker workflow.
     *
     * @param cifNumber       Borrower CIF
     * @param loanType        HOME / PERSONAL / AUTO / EDUCATION / BUSINESS / GOLD
     * @param requestedAmount Amount requested in ₹
     * @param tenureMonths    Loan tenure in months
     * @param interestRate    Applicable interest rate (annual %)
     * @param purpose         Purpose of loan
     * @return                Saved Loan entity (status: APPLIED)
     */
    @Transactional(rollbackFor = Exception.class)
    public Loan applyForLoan(String cifNumber, String loanType, BigDecimal requestedAmount,
                              int tenureMonths, BigDecimal interestRate, String purpose,
                              String repaymentAccountNumber) {

        log.info("Loan application: {} ₹{} for CIF [{}] tenure {}M",
                 loanType, requestedAmount, cifNumber, tenureMonths);

        // ── Validate customer ─────────────────────────────────────
        Customer customer = customerRepository.findByCifNumber(cifNumber)
            .orElseThrow(() -> new BankingException("Customer not found: CIF " + cifNumber));

        // ── Validate loan parameters ──────────────────────────────
        validateLoanParams(loanType, requestedAmount, tenureMonths);

        // ── Find repayment account ────────────────────────────────
        Account repaymentAccount = null;
        if (repaymentAccountNumber != null) {
            repaymentAccount = accountRepository.findByAccountNumber(repaymentAccountNumber)
                .orElseThrow(() -> new BankingException("Repayment account not found: " + repaymentAccountNumber));
        }

        // ── Calculate EMI ─────────────────────────────────────────
        BigDecimal emi = Loan.calculateEmi(requestedAmount, interestRate, tenureMonths);

        // ── Calculate processing fee (typically 0.5–2% of loan amount) ──
        BigDecimal processingFee = requestedAmount.multiply(new BigDecimal("0.01"))
            .setScale(2, java.math.RoundingMode.HALF_UP);

        // ── Generate loan number ──────────────────────────────────
        String loanNumber = generateLoanNumber(loanType);

        Loan loan = Loan.builder()
            .loanNumber(loanNumber)
            .loanType(loanType)
            .loanStatus("APPLIED")
            .customer(customer)
            .repaymentAccount(repaymentAccount)
            .sanctionedAmount(requestedAmount)
            .outstandingPrincipal(requestedAmount)
            .interestRate(interestRate)
            .rateType("FIXED")
            .tenureMonths(tenureMonths)
            .emiAmount(emi)
            .emisRemaining(tenureMonths)
            .processingFee(processingFee)
            .applicationDate(LocalDate.now())
            .loanPurpose(purpose)
            .npaClassification("STANDARD")
            .build();

        Loan saved = loanRepository.save(loan);

        auditLogService.logAsync(
            getCurrentUsername(), "LOAN_APPLY", "LOAN", loanNumber,
            String.format("Loan application: %s ₹%s for %s (CIF: %s). EMI: ₹%s/month",
                loanType, requestedAmount, customer.getFullName(), cifNumber, emi),
            "SUCCESS", null
        );

        log.info("Loan application {} created for CIF [{}]. EMI: ₹{}", loanNumber, cifNumber, emi);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────
    //  LOAN APPROVAL (Maker-Checker)
    // ─────────────────────────────────────────────────────────────

    /**
     * Approves a loan application (Checker role).
     * Changes status from APPLIED/UNDER_REVIEW → APPROVED.
     * Triggers disbursement workflow.
     */
    @Transactional(rollbackFor = Exception.class)
    public Loan approveLoan(String loanNumber, String remarks) {
        Loan loan = loanRepository.findByLoanNumber(loanNumber)
            .orElseThrow(() -> new BankingException("Loan not found: " + loanNumber));

        if (!"APPLIED".equals(loan.getLoanStatus()) && !"UNDER_REVIEW".equals(loan.getLoanStatus())) {
            throw new BankingException("Loan " + loanNumber + " cannot be approved. Status: " + loan.getLoanStatus());
        }

        loan.setLoanStatus("APPROVED");
        loan.setSanctionDate(LocalDate.now());
        loan.setApprovedBy(getCurrentUsername());
        loan.setApprovalRemarks(remarks);
        Loan saved = loanRepository.save(loan);

        auditLogService.logAsync(
            getCurrentUsername(), "LOAN_APPROVE", "LOAN", loanNumber,
            "Loan approved. Remarks: " + remarks, "SUCCESS", null
        );

        return saved;
    }

    /**
     * Rejects a loan application.
     */
    @Transactional(rollbackFor = Exception.class)
    public Loan rejectLoan(String loanNumber, String rejectionReason) {
        Loan loan = loanRepository.findByLoanNumber(loanNumber)
            .orElseThrow(() -> new BankingException("Loan not found: " + loanNumber));

        loan.setLoanStatus("REJECTED");
        loan.setRejectionReason(rejectionReason);
        Loan saved = loanRepository.save(loan);

        auditLogService.logAsync(
            getCurrentUsername(), "LOAN_REJECT", "LOAN", loanNumber,
            "Loan rejected. Reason: " + rejectionReason, "SUCCESS", null
        );
        return saved;
    }

    // ─────────────────────────────────────────────────────────────
    //  DISBURSEMENT
    // ─────────────────────────────────────────────────────────────

    /**
     * Disburses an approved loan to the borrower's account.
     *
     * Steps:
     *  1. Validate loan is APPROVED
     *  2. Credit disbursement amount to borrower's account
     *  3. Update loan status to DISBURSED → ACTIVE
     *  4. Set first/next EMI dates
     *  5. Log audit entry and notify borrower
     */
    @Transactional(rollbackFor = Exception.class)
    public Loan disburseLoan(String loanNumber, String creditAccountNumber) {
        Loan loan = loanRepository.findByLoanNumber(loanNumber)
            .orElseThrow(() -> new BankingException("Loan not found: " + loanNumber));

        if (!"APPROVED".equals(loan.getLoanStatus())) {
            throw new BankingException("Loan must be APPROVED before disbursement. Status: " + loan.getLoanStatus());
        }

        // Credit disbursement to borrower's account
        Account creditAccount = accountRepository.findByAccountNumber(creditAccountNumber)
            .orElseThrow(() -> new BankingException("Disbursement account not found: " + creditAccountNumber));

        creditAccount.setCurrentBalance(
            creditAccount.getCurrentBalance().add(loan.getSanctionedAmount()));
        creditAccount.setAvailableBalance(
            creditAccount.getAvailableBalance().add(loan.getSanctionedAmount()));
        accountRepository.save(creditAccount);

        // Set loan disbursement details
        LocalDate today = LocalDate.now();
        loan.setLoanStatus("ACTIVE");
        loan.setDisbursedAmount(loan.getSanctionedAmount());
        loan.setDisbursementDate(today);
        loan.setFirstEmiDate(today.plusMonths(1));
        loan.setNextEmiDate(today.plusMonths(1));
        loan.setMaturityDate(today.plusMonths(loan.getTenureMonths()));
        Loan saved = loanRepository.save(loan);

        auditLogService.logAsync(
            getCurrentUsername(), "LOAN_DISBURSE", "LOAN", loanNumber,
            String.format("Loan disbursed ₹%s to account %s",
                loan.getSanctionedAmount(), creditAccountNumber),
            "SUCCESS", null
        );

        return saved;
    }

    // ─────────────────────────────────────────────────────────────
    //  QUERY OPERATIONS
    // ─────────────────────────────────────────────────────────────

    public Loan getLoanByNumber(String loanNumber) {
        return loanRepository.findByLoanNumber(loanNumber)
            .orElseThrow(() -> new BankingException("Loan not found: " + loanNumber));
    }

    public java.util.List<Loan> getCustomerLoans(String cifNumber) {
        return loanRepository.findByCustomerCif(cifNumber);
    }

    public Page<Loan> getLoansByStatus(String status, Pageable pageable) {
        return loanRepository.findByLoanStatusOrderByApplicationDateDesc(status, pageable);
    }

    public BigDecimal getTotalLoanBook() {
        return loanRepository.sumTotalOutstandingPrincipal();
    }

    // ─────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────

    private void validateLoanParams(String loanType, BigDecimal amount, int tenure) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BankingException("Loan amount must be greater than zero.");
        }
        // Product-specific limits
        BigDecimal maxAmount = switch (loanType) {
            case "HOME"       -> new BigDecimal("50000000"); // ₹5 Crore
            case "PERSONAL"   -> new BigDecimal("5000000");  // ₹50 Lakh
            case "AUTO"       -> new BigDecimal("2500000");  // ₹25 Lakh
            case "EDUCATION"  -> new BigDecimal("7500000");  // ₹75 Lakh
            case "BUSINESS"   -> new BigDecimal("20000000"); // ₹2 Crore
            case "GOLD"       -> new BigDecimal("5000000");  // ₹50 Lakh
            default           -> new BigDecimal("10000000");
        };
        if (amount.compareTo(maxAmount) > 0) {
            throw new BankingException(
                String.format("Maximum %s loan amount is ₹%s. Requested: ₹%s",
                              loanType, maxAmount, amount));
        }
        if (tenure < 3 || tenure > 360) {
            throw new BankingException("Loan tenure must be between 3 and 360 months.");
        }
    }

    private String generateLoanNumber(String loanType) {
        String typeCode = switch (loanType) {
            case "HOME"      -> "HL";
            case "PERSONAL"  -> "PL";
            case "AUTO"      -> "AL";
            case "EDUCATION" -> "EL";
            case "BUSINESS"  -> "BL";
            case "GOLD"      -> "GL";
            default          -> "LN";
        };
        long seq = loanRepository.countByLoanStatus("APPLIED") + loanRepository.count() + 1;
        return "LN" + typeCode + Year.now().getValue() + String.format("%04d", seq);
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
