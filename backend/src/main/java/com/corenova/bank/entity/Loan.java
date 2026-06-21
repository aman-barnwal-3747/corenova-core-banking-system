package com.corenova.bank.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ================================================================
 *  Loan – Retail Loan / Credit Facility Entity
 *
 *  Supports all retail loan products offered by CoreNova Bank:
 *    • Home Loan        (HL)  – Up to ₹5 Crore, 30-year tenure
 *    • Personal Loan    (PL)  – Up to ₹50 Lakh, 7-year tenure
 *    • Auto Loan        (AL)  – Up to ₹25 Lakh, 7-year tenure
 *    • Education Loan   (EL)  – Up to ₹75 Lakh, 15-year tenure
 *    • Business Loan    (BL)  – Up to ₹2 Crore, 10-year tenure
 *    • Gold Loan        (GL)  – Up to ₹50 Lakh, 3-year tenure
 *    • Overdraft        (OD)  – Against collateral/FD
 *
 *  EMI Calculation (Reducing Balance Method):
 *    EMI = P × r × (1+r)^n / ((1+r)^n - 1)
 *    where P = principal, r = monthly rate, n = tenure in months
 *
 *  Loan Number Format: LN{LoanType}{Year}{Sequential}
 *    Example: LNHL20250001 (Home Loan opened in 2025)
 *
 *  In Finacle: Loans & Advances module (LAMOD).
 * ================================================================
 */
@Entity
@Table(
    name = "cbs_loans",
    indexes = {
        @Index(name = "idx_loan_number",   columnList = "loan_number"),
        @Index(name = "idx_loan_customer", columnList = "customer_id"),
        @Index(name = "idx_loan_status",   columnList = "loan_status"),
        @Index(name = "idx_loan_type",     columnList = "loan_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loan_id")
    private Long loanId;

    // ── Loan Identity ────────────────────────────────────────────

    /** Unique loan reference number (e.g. LNHL20250001). */
    @Column(name = "loan_number", unique = true, nullable = false, length = 20)
    private String loanNumber;

    /**
     * Loan product type.
     * Values: HOME, PERSONAL, AUTO, EDUCATION, BUSINESS, GOLD, OVERDRAFT
     */
    @Column(name = "loan_type", nullable = false, length = 20)
    private String loanType;

    /**
     * Loan lifecycle status:
     *   APPLIED → UNDER_REVIEW → APPROVED → DISBURSED → ACTIVE →
     *   CLOSED | DEFAULTED | WRITTEN_OFF | RESTRUCTURED
     */
    @Column(name = "loan_status", nullable = false, length = 20)
    @Builder.Default
    private String loanStatus = "APPLIED";

    // ── Borrower Link ────────────────────────────────────────────

    /** Customer who took the loan (CIF link). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** Repayment account — EMI auto-debited from this account on due date. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repayment_account_id")
    private Account repaymentAccount;

    // ── Financial Terms ──────────────────────────────────────────

    /** Sanctioned loan amount (₹). */
    @Column(name = "sanctioned_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal sanctionedAmount;

    /** Amount actually disbursed to borrower (may be less than sanctioned). */
    @Column(name = "disbursed_amount", precision = 15, scale = 2)
    private BigDecimal disbursedAmount;

    /** Current outstanding principal balance. */
    @Column(name = "outstanding_principal", precision = 15, scale = 2)
    private BigDecimal outstandingPrincipal;

    /** Overdue principal amount (for NPA classification). */
    @Column(name = "overdue_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal overdueAmount = BigDecimal.ZERO;

    /**
     * Annual interest rate (e.g. 8.50 for 8.5% per annum).
     * For floating rate loans, updated quarterly per RBI MCLR.
     */
    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    /** Rate type: FIXED or FLOATING (linked to MCLR). */
    @Column(name = "rate_type", length = 10)
    @Builder.Default
    private String rateType = "FIXED";

    /** Loan tenure in months (e.g. 240 for 20-year home loan). */
    @Column(name = "tenure_months", nullable = false)
    private Integer tenureMonths;

    /**
     * Monthly EMI amount (calculated at sanction).
     * EMI = P × r × (1+r)^n / ((1+r)^n - 1)
     */
    @Column(name = "emi_amount", precision = 12, scale = 2)
    private BigDecimal emiAmount;

    /** Number of EMIs already paid. */
    @Column(name = "emis_paid")
    @Builder.Default
    private Integer emisPaid = 0;

    /** Total EMIs remaining = tenureMonths - emisPaid. */
    @Column(name = "emis_remaining")
    private Integer emisRemaining;

    // ── Dates ────────────────────────────────────────────────────

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    @Column(name = "sanction_date")
    private LocalDate sanctionDate;

    @Column(name = "disbursement_date")
    private LocalDate disbursementDate;

    /** First EMI due date (typically 1 month after disbursement). */
    @Column(name = "first_emi_date")
    private LocalDate firstEmiDate;

    /** Next EMI due date (for auto-debit scheduling). */
    @Column(name = "next_emi_date")
    private LocalDate nextEmiDate;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(name = "closure_date")
    private LocalDate closureDate;

    // ── Collateral / Security ────────────────────────────────────

    /**
     * Collateral description for secured loans.
     * Examples: "Property at 12 MG Road, Mumbai" or "FD A/C 1001023456"
     */
    @Column(name = "collateral_description", length = 500)
    private String collateralDescription;

    @Column(name = "collateral_value", precision = 15, scale = 2)
    private BigDecimal collateralValue;

    /** Loan-to-Value ratio (LTV) = disbursedAmount / collateralValue × 100 */
    @Column(name = "ltv_ratio", precision = 5, scale = 2)
    private BigDecimal ltvRatio;

    // ── Approval Workflow ────────────────────────────────────────

    /** Loan officer who processed the application. */
    @Column(name = "processed_by", length = 100)
    private String processedBy;

    /** Approving authority (branch manager / regional manager). */
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approval_remarks", length = 500)
    private String approvalRemarks;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // ── NPA Classification ───────────────────────────────────────

    /**
     * Number of consecutive EMIs missed (NPA trigger).
     * RBI Rule: NPA classification after 90 days (3 EMIs) of non-payment.
     */
    @Column(name = "overdue_emis")
    @Builder.Default
    private Integer overdueEmis = 0;

    /**
     * NPA (Non-Performing Asset) classification:
     *   STANDARD → SUB_STANDARD → DOUBTFUL → LOSS
     * Updated by the EOD NPA classification job.
     */
    @Column(name = "npa_classification", length = 20)
    @Builder.Default
    private String npaClassification = "STANDARD";

    // ── Pre-closure ──────────────────────────────────────────────

    /** Pre-closure penalty % (typically 2-3% of outstanding principal). */
    @Column(name = "preclosure_penalty_pct", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal preclosurePenaltyPct = new BigDecimal("2.00");

    // ── Processing Charges ───────────────────────────────────────

    /** One-time processing fee charged at disbursement. */
    @Column(name = "processing_fee", precision = 10, scale = 2)
    private BigDecimal processingFee;

    @Column(name = "insurance_amount", precision = 10, scale = 2)
    private BigDecimal insuranceAmount;

    // ── Purpose & Notes ──────────────────────────────────────────

    @Column(name = "loan_purpose", length = 200)
    private String loanPurpose;

    @Column(name = "loan_remarks", length = 500)
    private String loanRemarks;

    // ── Helper methods ───────────────────────────────────────────

    /** Returns true if loan is active and EMIs are current. */
    public boolean isActive() {
        return "ACTIVE".equals(loanStatus) || "DISBURSED".equals(loanStatus);
    }

    /** Returns true if loan is NPA (≥3 EMIs overdue). */
    public boolean isNpa() {
        return overdueEmis >= 3;
    }

    /** Calculates EMI using the reducing balance formula. */
    public static BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRate, int tenureMonths) {
        double P = principal.doubleValue();
        double r = annualRate.doubleValue() / (12 * 100); // Monthly rate
        int    n = tenureMonths;

        if (r == 0) return principal.divide(BigDecimal.valueOf(n), 2, java.math.RoundingMode.HALF_UP);

        double emi = P * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1);
        return BigDecimal.valueOf(emi).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
