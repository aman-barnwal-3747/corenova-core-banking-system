package com.corenova.bank.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * ================================================================
 *  Beneficiary – Saved Payee / Transfer Beneficiary
 *  PDF §8 Payment Gateway Workflow
 *
 *  Beneficiaries are pre-validated payees saved by a customer
 *  to speed up repeat transfers without re-entering account details.
 *
 *  Types:
 *   INTERNAL  – Another CoreNova Bank account
 *   EXTERNAL  – Account at another bank (NEFT/RTGS/IMPS)
 *   UPI       – UPI Virtual Payment Address (VPA)
 *
 *  Validation (at add time, not at transfer time):
 *   • Account number + IFSC combination validated with RBI NPCI
 *   • Name verified against bank records (penny-drop verification)
 *   • Cooling period: 24 hours before first transfer (fraud prevention)
 *
 *  In Finacle: Beneficiary Management module (BENMGT).
 * ================================================================
 */
@Entity
@Table(
    name = "cbs_beneficiaries",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"account_id", "beneficiary_account_number"},
        name        = "uq_account_beneficiary"
    ),
    indexes = {
        @Index(name = "idx_ben_account",  columnList = "account_id"),
        @Index(name = "idx_ben_type",     columnList = "beneficiary_type"),
        @Index(name = "idx_ben_active",   columnList = "is_active")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Beneficiary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "beneficiary_id")
    private Long beneficiaryId;

    // ── Linked Account (owner of this beneficiary list) ──────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // ── Beneficiary Type ─────────────────────────────────────────
    /**
     * INTERNAL : CoreNova-to-CoreNova transfer
     * EXTERNAL : NEFT/RTGS/IMPS to other bank
     * UPI      : UPI VPA payment
     */
    @Column(name = "beneficiary_type", nullable = false, length = 10)
    private String beneficiaryType; // INTERNAL | EXTERNAL | UPI

    // ── Beneficiary Identity ─────────────────────────────────────
    /** Friendly name given by the account holder (e.g. "Mom", "Office Rent"). */
    @Column(name = "nickname", length = 50)
    private String nickname;

    /** Full name as per beneficiary's bank records. */
    @Column(name = "beneficiary_name", nullable = false, length = 100)
    private String beneficiaryName;

    // ── Bank Details (EXTERNAL / INTERNAL) ───────────────────────
    @Column(name = "beneficiary_account_number", length = 20)
    private String beneficiaryAccountNumber;

    /** IFSC code of beneficiary's bank branch. */
    @Column(name = "beneficiary_ifsc", length = 15)
    private String beneficiaryIfsc;

    @Column(name = "beneficiary_bank_name", length = 100)
    private String beneficiaryBankName;

    @Column(name = "beneficiary_branch_name", length = 100)
    private String beneficiaryBranchName;

    // ── UPI Details ───────────────────────────────────────────────
    /** UPI Virtual Payment Address (e.g. priya@paytm). UPI type only. */
    @Column(name = "upi_id", length = 50)
    private String upiId;

    // ── Limits & Controls ─────────────────────────────────────────
    /**
     * Per-transaction limit for this beneficiary.
     * Provides fine-grained control beyond account-level daily limit.
     * Set to null for no override (use account's daily limit).
     */
    @Column(name = "transfer_limit", precision = 15, scale = 2)
    private java.math.BigDecimal transferLimit;

    /** 24-hour cooling period — no transfers allowed until this time. */
    @Column(name = "cooling_period_ends_at")
    private java.time.LocalDateTime coolingPeriodEndsAt;

    /** Whether cooling period has been completed and transfers are allowed. */
    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    // ── Usage Stats ───────────────────────────────────────────────
    @Column(name = "total_transfers")
    @Builder.Default
    private Integer totalTransfers = 0;

    @Column(name = "last_transfer_at")
    private java.time.LocalDateTime lastTransferAt;

    @Column(name = "total_amount_transferred", precision = 15, scale = 2)
    @Builder.Default
    private java.math.BigDecimal totalAmountTransferred = java.math.BigDecimal.ZERO;

    // ── Helper ────────────────────────────────────────────────────
    public boolean isTransferAllowed() {
        if (!Boolean.TRUE.equals(isActive)) return false;
        if (!Boolean.TRUE.equals(isVerified)) return false;
        if (coolingPeriodEndsAt != null &&
            java.time.LocalDateTime.now().isBefore(coolingPeriodEndsAt)) return false;
        return true;
    }

    public String getDisplayName() {
        return nickname != null && !nickname.isBlank() ? nickname : beneficiaryName;
    }
}
