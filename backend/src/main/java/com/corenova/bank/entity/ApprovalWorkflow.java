package com.corenova.bank.entity;

import com.corenova.bank.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ================================================================
 *  ApprovalWorkflow – Maker-Checker Approval Request Entity
 *
 *  The Maker-Checker pattern is a MANDATORY RBI compliance control
 *  for all high-value or sensitive banking operations:
 *
 *  HIGH-VALUE TRIGGERS (require checker approval):
 *   • Fund transfers ≥ ₹5 Lakh
 *   • Account freeze / unfreeze
 *   • Loan sanction
 *   • KYC approval
 *   • New account opening for HNI/CORPORATE segment
 *   • User role changes
 *
 *  WORKFLOW:
 *   Maker (Teller) → Creates request (status: PENDING)
 *   Checker (Manager) → APPROVES or REJECTS
 *   On APPROVE → System executes the original action
 *   On REJECT  → Request archived; maker notified
 *
 *  SLA: Pending approvals auto-expire after 24 hours (EXPIRED).
 *  In Finacle: Maker-Checker (MKCR) control module.
 * ================================================================
 */
@Entity
@Table(
    name = "cbs_approval_workflows",
    indexes = {
        @Index(name = "idx_approval_status",  columnList = "approval_status"),
        @Index(name = "idx_approval_maker",   columnList = "created_by_maker"),
        @Index(name = "idx_approval_checker", columnList = "checked_by"),
        @Index(name = "idx_approval_entity",  columnList = "entity_type, entity_ref"),
        @Index(name = "idx_approval_created", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalWorkflow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "approval_id")
    private Long approvalId;

    // ── Approval Reference ───────────────────────────────────────

    /** Unique reference for this approval request (e.g. APR20250001). */
    @Column(name = "approval_ref", unique = true, nullable = false, length = 20)
    private String approvalRef;

    // ── Action Description ───────────────────────────────────────

    /**
     * Type of action awaiting approval.
     * Examples: FUND_TRANSFER, ACCOUNT_FREEZE, LOAN_SANCTION,
     *           KYC_APPROVE, ACCOUNT_OPEN, USER_ROLE_CHANGE
     */
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    /** Human-readable description of the action for checker review. */
    @Column(name = "action_description", length = 500)
    private String actionDescription;

    // ── Subject Entity ───────────────────────────────────────────

    /** The entity type being acted upon (ACCOUNT, CUSTOMER, LOAN, USER). */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /** The specific entity reference (e.g. account number, CIF, loan number). */
    @Column(name = "entity_ref", length = 50)
    private String entityRef;

    /**
     * JSON-serialised payload of the original request.
     * Stored so the system can re-execute the action after approval
     * without requiring the maker to re-enter data.
     */
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    // ── Financial Context ────────────────────────────────────────

    /** If applicable — amount involved in the action (e.g. transfer amount). */
    @Column(name = "amount_involved", precision = 15, scale = 2)
    private BigDecimal amountInvolved;

    /** Risk level assessed at creation: LOW / MEDIUM / HIGH / CRITICAL. */
    @Column(name = "risk_level", length = 10)
    @Builder.Default
    private String riskLevel = "MEDIUM";

    // ── Workflow Parties ─────────────────────────────────────────

    /** Employee ID of the Maker (who created this request). */
    @Column(name = "created_by_maker", nullable = false, length = 50)
    private String createdByMaker;

    /** Branch code of the Maker. */
    @Column(name = "maker_branch_code", length = 20)
    private String makerBranchCode;

    /** Employee ID of the Checker (who approved or rejected). */
    @Column(name = "checked_by", length = 50)
    private String checkedBy;

    /** Branch code of the Checker. */
    @Column(name = "checker_branch_code", length = 20)
    private String checkerBranchCode;

    // ── Status & Dates ───────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 15)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    /** Maker's notes / justification for the action. */
    @Column(name = "maker_remarks", length = 500)
    private String makerRemarks;

    /** Checker's notes explaining approval/rejection decision. */
    @Column(name = "checker_remarks", length = 500)
    private String checkerRemarks;

    /** When the checker acted on this request. */
    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    /**
     * SLA deadline — request auto-expires at this time.
     * Default: createdAt + 24 hours.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    // ── Execution Result ─────────────────────────────────────────

    /** Reference number of the action executed after approval. */
    @Column(name = "executed_ref", length = 50)
    private String executedRef;

    /** Whether the post-approval action was executed successfully. */
    @Column(name = "execution_success")
    private Boolean executionSuccess;

    @Column(name = "execution_error", length = 300)
    private String executionError;

    /** When the approved action was executed. */
    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    // ── Priority ─────────────────────────────────────────────────

    /** Priority for checker queue: 1 = Critical, 5 = Low */
    @Column(name = "priority")
    @Builder.Default
    private Integer priority = 3;
}
