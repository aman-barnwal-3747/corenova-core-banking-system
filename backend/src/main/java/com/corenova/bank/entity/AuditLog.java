package com.corenova.bank.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ================================================================
 *  AuditLog – System-Wide Compliance Audit Trail
 *
 *  Every significant action in CoreNova Bank is recorded here.
 *  This is an APPEND-ONLY table — records are NEVER updated or deleted.
 *
 *  Required for:
 *    • RBI regulatory compliance (Section 43A IT Act)
 *    • Fraud investigation
 *    • Internal audit reviews
 *    • SOX compliance (if applicable)
 *
 *  Audit entries are written asynchronously to avoid impacting
 *  transaction performance (via @Async audit service).
 *
 *  Example entry:
 *    USER [aman.verma] ACTION [FUND_TRANSFER] ON [ACCOUNT:1234567890]
 *    AMOUNT [₹25,000] STATUS [SUCCESS] IP [192.168.1.1] AT [2025-05-20 10:30:00]
 *
 *  In Finacle, this maps to the Audit Trail module (AEOD/BEOD logs).
 * ================================================================
 */
@Entity
@Table(
    name = "cbs_audit_logs",
    indexes = {
        @Index(name = "idx_audit_user",     columnList = "performed_by"),
        @Index(name = "idx_audit_action",   columnList = "action"),
        @Index(name = "idx_audit_entity",   columnList = "entity_type, entity_id"),
        @Index(name = "idx_audit_time",     columnList = "performed_at"),
        @Index(name = "idx_audit_status",   columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    // ── Who ─────────────────────────────────────────────────────

    /** Username / employee ID who performed the action. */
    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "user_role", length = 20)
    private String userRole;

    // ── What Action ──────────────────────────────────────────────

    /**
     * Action code — standardized verb (uppercase).
     * Examples: LOGIN, LOGOUT, ACCOUNT_OPEN, FUND_TRANSFER,
     *           KYC_APPROVE, PASSWORD_CHANGE, ACCOUNT_FREEZE
     */
    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "module", length = 50)
    private String module; // AUTH, ACCOUNT, TRANSACTION, CUSTOMER, LOAN, etc.

    // ── On What Entity ───────────────────────────────────────────

    @Column(name = "entity_type", length = 50)
    private String entityType; // ACCOUNT, CUSTOMER, TRANSACTION, USER

    @Column(name = "entity_id", length = 50)
    private String entityId;   // The specific record affected

    // ── Description ──────────────────────────────────────────────

    /**
     * Human-readable description of the action.
     * Example: "Fund transfer of ₹25,000 to Priya Sharma (HDFC0001234)"
     */
    @Column(name = "description", length = 1000)
    private String description;

    /** JSON snapshot of entity state BEFORE the action (for rollback analysis). */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /** JSON snapshot of entity state AFTER the action. */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    // ── Outcome ──────────────────────────────────────────────────

    /** SUCCESS or FAILURE */
    @Column(name = "status", length = 10)
    private String status;

    /** Error message if action failed. */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    // ── Technical Context ────────────────────────────────────────

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "request_id", length = 50)
    private String requestId;

    /** Branch code where action was performed. */
    @Column(name = "branch_code", length = 20)
    private String branchCode;

    // ── Timestamp ────────────────────────────────────────────────

    /** Exact timestamp — recorded server-side (not client-side). */
    @Column(name = "performed_at", nullable = false)
    @Builder.Default
    private LocalDateTime performedAt = LocalDateTime.now();
}
