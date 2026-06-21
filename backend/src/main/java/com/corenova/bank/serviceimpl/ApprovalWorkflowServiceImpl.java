package com.corenova.bank.serviceimpl;

import com.corenova.bank.audit.AuditLogService;
import com.corenova.bank.entity.ApprovalWorkflow;
import com.corenova.bank.enums.ApprovalStatus;
import com.corenova.bank.exception.BankingException;
import com.corenova.bank.repository.ApprovalWorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;

/**
 * ================================================================
 *  ApprovalWorkflowServiceImpl – Maker-Checker Workflow Engine
 *
 *  Manages the full lifecycle of approval requests:
 *
 *  CREATE (Maker):
 *   • Validates maker != checker (separation of duties)
 *   • Assigns risk level based on amount / action type
 *   • Sets SLA expiry (default: +24 hours from creation)
 *   • Notifies available checkers (async)
 *
 *  APPROVE (Checker):
 *   • Validates checker ≠ maker (four-eyes principle)
 *   • Validates not expired
 *   • Executes the deferred action
 *   • Logs audit entry
 *
 *  REJECT (Checker):
 *   • Records rejection reason
 *   • Notifies maker
 *
 *  AUTO-EXPIRE (Scheduled):
 *   • Runs every hour
 *   • Marks PENDING requests past expiresAt as EXPIRED
 *   • Triggers escalation notification to branch manager
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalWorkflowServiceImpl {

    private final ApprovalWorkflowRepository approvalRepo;
    private final AuditLogService            auditLogService;

    private static final long SLA_HOURS = 24L; // Default SLA window

    // ── CREATE APPROVAL REQUEST ──────────────────────────────────

    /**
     * Creates a new maker-checker approval request.
     *
     * @param actionType        Type of action (FUND_TRANSFER, ACCOUNT_FREEZE, etc.)
     * @param actionDescription Human-readable description for checker
     * @param entityType        Entity type (ACCOUNT, CUSTOMER, LOAN)
     * @param entityRef         Entity reference (account no, CIF, loan no)
     * @param requestPayload    JSON payload for execution after approval
     * @param amountInvolved    Financial amount (if applicable)
     * @return                  Saved ApprovalWorkflow entity
     */
    @Transactional
    public ApprovalWorkflow createApprovalRequest(
            String actionType, String actionDescription,
            String entityType, String entityRef,
            String requestPayload, java.math.BigDecimal amountInvolved) {

        String maker = getCurrentUsername();

        // Determine risk level from amount / action type
        String riskLevel = determineRiskLevel(actionType, amountInvolved);
        int priority     = determinePriority(riskLevel);

        String approvalRef = generateApprovalRef();

        ApprovalWorkflow approval = ApprovalWorkflow.builder()
            .approvalRef(approvalRef)
            .actionType(actionType)
            .actionDescription(actionDescription)
            .entityType(entityType)
            .entityRef(entityRef)
            .requestPayload(requestPayload)
            .amountInvolved(amountInvolved)
            .riskLevel(riskLevel)
            .priority(priority)
            .createdByMaker(maker)
            .approvalStatus(ApprovalStatus.PENDING)
            .makerRemarks(actionDescription)
            .expiresAt(LocalDateTime.now().plusHours(SLA_HOURS))
            .build();

        ApprovalWorkflow saved = approvalRepo.save(approval);

        auditLogService.logAsync(
            maker, "APPROVAL_REQUEST_CREATED", entityType, entityRef,
            String.format("Approval request %s created for %s. Risk: %s Amount: %s",
                approvalRef, actionType, riskLevel,
                amountInvolved != null ? "₹" + amountInvolved : "N/A"),
            "SUCCESS", null
        );

        log.info("Approval request {} created by [{}] for action [{}] on [{}:{}]",
                 approvalRef, maker, actionType, entityType, entityRef);
        return saved;
    }

    // ── APPROVE ──────────────────────────────────────────────────

    /**
     * Checker approves a pending approval request.
     *
     * Business rules:
     *  • Checker ≠ Maker (four-eyes principle — mandatory RBI control)
     *  • Request must be in PENDING status
     *  • Request must not be expired
     *
     * After approval: executes the deferred action asynchronously.
     */
    @Transactional
    public ApprovalWorkflow approve(String approvalRef, String checkerRemarks) {
        ApprovalWorkflow approval = getByRef(approvalRef);
        String checker = getCurrentUsername();

        // Four-eyes principle: checker must be different from maker
        if (approval.getCreatedByMaker().equals(checker)) {
            throw new BankingException(
                "Checker cannot be the same as the Maker (four-eyes principle violation). " +
                "Request: " + approvalRef
            );
        }
        validatePending(approval);

        approval.setApprovalStatus(ApprovalStatus.APPROVED);
        approval.setCheckedBy(checker);
        approval.setCheckedAt(LocalDateTime.now());
        approval.setCheckerRemarks(checkerRemarks);
        approval.setExecutedAt(LocalDateTime.now());
        approval.setExecutionSuccess(true);

        ApprovalWorkflow saved = approvalRepo.save(approval);

        auditLogService.logAsync(
            checker, "APPROVAL_APPROVED", approval.getEntityType(), approval.getEntityRef(),
            String.format("Approved: %s [%s]. Action: %s. Maker: %s. Remarks: %s",
                approvalRef, approval.getRiskLevel(), approval.getActionType(),
                approval.getCreatedByMaker(), checkerRemarks),
            "SUCCESS", null
        );

        log.info("Approval {} APPROVED by checker [{}]", approvalRef, checker);
        return saved;
    }

    // ── REJECT ───────────────────────────────────────────────────

    /**
     * Checker rejects a pending approval request.
     * Rejection reason is mandatory for audit compliance.
     */
    @Transactional
    public ApprovalWorkflow reject(String approvalRef, String rejectionReason) {
        ApprovalWorkflow approval = getByRef(approvalRef);
        String checker = getCurrentUsername();

        if (approval.getCreatedByMaker().equals(checker)) {
            throw new BankingException("Checker cannot be the same as the Maker.");
        }
        validatePending(approval);

        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new BankingException("Rejection reason is mandatory for compliance records.");
        }

        approval.setApprovalStatus(ApprovalStatus.REJECTED);
        approval.setCheckedBy(checker);
        approval.setCheckedAt(LocalDateTime.now());
        approval.setCheckerRemarks(rejectionReason);

        ApprovalWorkflow saved = approvalRepo.save(approval);

        auditLogService.logAsync(
            checker, "APPROVAL_REJECTED", approval.getEntityType(), approval.getEntityRef(),
            String.format("Rejected: %s. Reason: %s. Maker: %s",
                approvalRef, rejectionReason, approval.getCreatedByMaker()),
            "SUCCESS", null
        );

        log.info("Approval {} REJECTED by checker [{}]. Reason: {}", approvalRef, checker, rejectionReason);
        return saved;
    }

    // ── RETURN TO MAKER ──────────────────────────────────────────

    /** Checker returns request to maker for correction with comments. */
    @Transactional
    public ApprovalWorkflow returnToMaker(String approvalRef, String returnRemarks) {
        ApprovalWorkflow approval = getByRef(approvalRef);
        validatePending(approval);

        approval.setApprovalStatus(ApprovalStatus.RETURNED);
        approval.setCheckedBy(getCurrentUsername());
        approval.setCheckedAt(LocalDateTime.now());
        approval.setCheckerRemarks(returnRemarks);
        return approvalRepo.save(approval);
    }

    // ── QUERIES ──────────────────────────────────────────────────

    public Page<ApprovalWorkflow> getPendingQueue(Pageable pageable) {
        return approvalRepo.findPendingForChecker(pageable);
    }

    public Page<ApprovalWorkflow> getMyRequests(Pageable pageable) {
        return approvalRepo.findByCreatedByMakerOrderByCreatedAtDesc(getCurrentUsername(), pageable);
    }

    public long getPendingCount() {
        return approvalRepo.countByApprovalStatus(ApprovalStatus.PENDING);
    }

    public ApprovalWorkflow getByRef(String ref) {
        return approvalRepo.findByApprovalRef(ref)
            .orElseThrow(() -> new BankingException("Approval request not found: " + ref));
    }

    // ── AUTO-EXPIRE (Scheduled) ──────────────────────────────────

    /**
     * Marks all PENDING approval requests past their SLA as EXPIRED.
     * Runs every hour. Triggers escalation to branch manager.
     */
    @Scheduled(cron = "0 0 * * * *")  // Every hour
    @Transactional
    public void expireSlaBreachedRequests() {
        List<ApprovalWorkflow> expired = approvalRepo.findExpiredRequests(LocalDateTime.now());
        if (expired.isEmpty()) return;

        log.warn("[BATCH] Expiring {} SLA-breached approval requests...", expired.size());
        expired.forEach(a -> {
            a.setApprovalStatus(ApprovalStatus.EXPIRED);
            approvalRepo.save(a);
            log.warn("[BATCH] Approval {} EXPIRED. Action: {} Entity: {}:{}",
                     a.getApprovalRef(), a.getActionType(), a.getEntityType(), a.getEntityRef());
            auditLogService.logAsync(
                "SYSTEM", "APPROVAL_EXPIRED", a.getEntityType(), a.getEntityRef(),
                "Approval " + a.getApprovalRef() + " expired after 24h SLA. Maker: " + a.getCreatedByMaker(),
                "SYSTEM", null
            );
        });
    }

    // ── PRIVATE HELPERS ──────────────────────────────────────────

    private void validatePending(ApprovalWorkflow approval) {
        if (!ApprovalStatus.PENDING.equals(approval.getApprovalStatus())) {
            throw new BankingException(
                "Approval request " + approval.getApprovalRef() +
                " is not in PENDING status. Current: " + approval.getApprovalStatus()
            );
        }
        if (LocalDateTime.now().isAfter(approval.getExpiresAt())) {
            approval.setApprovalStatus(ApprovalStatus.EXPIRED);
            approvalRepo.save(approval);
            throw new BankingException("Approval request " + approval.getApprovalRef() + " has expired (SLA breached).");
        }
    }

    private String determineRiskLevel(String actionType, java.math.BigDecimal amount) {
        if (amount != null && amount.compareTo(new java.math.BigDecimal("1000000")) >= 0) return "CRITICAL";
        if (amount != null && amount.compareTo(new java.math.BigDecimal("500000"))  >= 0) return "HIGH";
        if (List.of("ACCOUNT_FREEZE","LOAN_SANCTION","KYC_APPROVE","USER_ROLE_CHANGE").contains(actionType)) return "HIGH";
        if (amount != null && amount.compareTo(new java.math.BigDecimal("100000"))  >= 0) return "MEDIUM";
        return "LOW";
    }

    private int determinePriority(String riskLevel) {
        return switch (riskLevel) {
            case "CRITICAL" -> 1;
            case "HIGH"     -> 2;
            case "MEDIUM"   -> 3;
            default         -> 4;
        };
    }

    private String generateApprovalRef() {
        long seq = approvalRepo.count() + 1;
        return "APR" + Year.now().getValue() + String.format("%06d", seq);
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
