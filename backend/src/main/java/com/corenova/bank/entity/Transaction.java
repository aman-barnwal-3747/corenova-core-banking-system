package com.corenova.bank.entity;

import com.corenova.bank.enums.TransactionStatus;
import com.corenova.bank.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ================================================================
 *  Transaction – Financial Transaction Record
 *
 *  Immutable audit record of every financial event in the system.
 *  Once COMMITTED, a transaction record is NEVER modified.
 *  Corrections are done via REVERSAL transactions (new record).
 *
 *  Transaction Reference Format: TXN + YYYYMMdd + Sequential (8 digits)
 *  Example: TXN202505200000123456
 *
 *  Double-Entry Principle:
 *    Every debit on sender account creates a credit on receiver account.
 *    Both records share the same referenceNumber for traceability.
 *
 *  In Finacle, this maps to the Transaction Journal in the GL module.
 * ================================================================
 */
@Entity
@Table(
    name = "cbs_transactions",
    indexes = {
        @Index(name = "idx_txn_ref",       columnList = "reference_number"),
        @Index(name = "idx_txn_account",   columnList = "account_id"),
        @Index(name = "idx_txn_status",    columnList = "transaction_status"),
        @Index(name = "idx_txn_type",      columnList = "transaction_type"),
        @Index(name = "idx_txn_date",      columnList = "transaction_date"),
        @Index(name = "idx_txn_utr",       columnList = "utr_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    // ── Reference Numbers ────────────────────────────────────────

    /**
     * Unique system-generated transaction reference.
     * Format: TXN{YYYYMMdd}{SequentialNo}
     * This is shared with the customer on SMS/email notifications.
     */
    @Column(name = "reference_number", unique = true, nullable = false, length = 30)
    private String referenceNumber;

    /**
     * UTR (Unique Transaction Reference) — RBI standard identifier.
     * Used for NEFT/RTGS inter-bank reconciliation.
     * Format varies by payment rail (NEFT/RTGS/IMPS/UPI).
     */
    @Column(name = "utr_number", length = 30)
    private String utrNumber;

    // ── Account ──────────────────────────────────────────────────

    /** Account on which this transaction is posted (debit or credit). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // ── Transaction Details ──────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 25)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_status", nullable = false, length = 25)
    @Builder.Default
    private TransactionStatus transactionStatus = TransactionStatus.INITIATED;

    /**
     * CREDIT = money coming INTO the account (e.g. salary, received transfer).
     * DEBIT  = money going OUT of the account (e.g. payment, withdrawal).
     */
    @Column(name = "entry_type", nullable = false, length = 6)
    private String entryType; // "CREDIT" or "DEBIT"

    /** Transaction amount — always positive (entryType determines direction). */
    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /** Ledger balance after this transaction was posted. */
    @Column(name = "balance_after_transaction", precision = 15, scale = 2)
    private BigDecimal balanceAfterTransaction;

    /** ISO 4217 currency code (default INR). */
    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "INR";

    // ── Counterparty Details ─────────────────────────────────────
    // (Used for transfers: who sent/received the money)

    @Column(name = "counterparty_account_number", length = 20)
    private String counterpartyAccountNumber;

    @Column(name = "counterparty_name", length = 100)
    private String counterpartyName;

    @Column(name = "counterparty_bank_name", length = 100)
    private String counterpartyBankName;

    /** IFSC code of counterparty bank branch (for NEFT/RTGS). */
    @Column(name = "counterparty_ifsc", length = 15)
    private String counterpartyIfsc;

    /** UPI ID of counterparty (for UPI transactions). */
    @Column(name = "counterparty_upi_id", length = 50)
    private String counterpartyUpiId;

    // ── Narration ────────────────────────────────────────────────

    /** Transaction narration shown on account statement. */
    @Column(name = "narration", length = 200)
    private String narration;

    /** Payment purpose / remarks entered by initiator. */
    @Column(name = "remarks", length = 500)
    private String remarks;

    // ── Timing ───────────────────────────────────────────────────

    /** When the transaction was initiated / value-dated. */
    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    /** When settlement was confirmed (differs from initiation for NEFT batch). */
    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;

    /** Value date (for interest calculation on FD/RD). */
    @Column(name = "value_date")
    private java.time.LocalDate valueDate;

    // ── Charges / Taxes ──────────────────────────────────────────

    /** Bank service charge for this transaction (e.g. RTGS fee). */
    @Column(name = "charge_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal chargeAmount = BigDecimal.ZERO;

    /** GST on charges (18% as per Indian tax law). */
    @Column(name = "gst_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal gstAmount = BigDecimal.ZERO;

    // ── Reversal Link ────────────────────────────────────────────

    /** If this is a reversal, points to the original transaction. */
    @Column(name = "original_transaction_ref", length = 30)
    private String originalTransactionRef;

    // ── Channel & Device ─────────────────────────────────────────

    /**
     * Channel through which transaction was initiated.
     * Values: INTERNET_BANKING, MOBILE_APP, CBS_COUNTER, ATM, API, BATCH
     */
    @Column(name = "channel", length = 30)
    private String channel;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "device_id", length = 100)
    private String deviceId;

    // ── Maker-Checker ────────────────────────────────────────────

    /** Employee who initiated the transaction (Maker in maker-checker). */
    @Column(name = "initiated_by", length = 50)
    private String initiatedBy;

    /** Manager who approved the transaction (Checker). */
    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    // ── Failure Info ─────────────────────────────────────────────

    /** Error code if transaction failed (for reconciliation). */
    @Column(name = "failure_reason", length = 200)
    private String failureReason;
}
