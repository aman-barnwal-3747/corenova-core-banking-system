package com.corenova.bank.repository;

import com.corenova.bank.entity.ApprovalWorkflow;
import com.corenova.bank.enums.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ApprovalWorkflowRepository – Maker-Checker queue queries.
 *
 * Critical queries:
 *  • pendingForChecker : Main checker queue (sorted by priority, then age)
 *  • expiredApprovals  : SLA-expired requests (scheduled cleanup)
 *  • myMakerRequests   : Maker's own submitted requests
 */
@Repository
public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {

    Optional<ApprovalWorkflow> findByApprovalRef(String approvalRef);

    /**
     * Returns the checker's pending queue.
     * Sorted by priority (1=Critical first), then oldest first (FIFO).
     */
    @Query("SELECT a FROM ApprovalWorkflow a WHERE a.approvalStatus = 'PENDING' " +
           "ORDER BY a.priority ASC, a.createdAt ASC")
    Page<ApprovalWorkflow> findPendingForChecker(Pageable pageable);

    /** All requests created by a specific maker. */
    Page<ApprovalWorkflow> findByCreatedByMakerOrderByCreatedAtDesc(String makerUsername, Pageable pageable);

    /** Requests by action type — for audit / compliance reporting. */
    Page<ApprovalWorkflow> findByActionTypeAndApprovalStatusOrderByCreatedAtDesc(
        String actionType, ApprovalStatus status, Pageable pageable);

    /** SLA-expired requests that need to be closed by the scheduled job. */
    @Query("SELECT a FROM ApprovalWorkflow a WHERE a.approvalStatus = 'PENDING' AND a.expiresAt < :now")
    List<ApprovalWorkflow> findExpiredRequests(@Param("now") LocalDateTime now);

    /** Count of pending approvals for dashboard KPI. */
    long countByApprovalStatus(ApprovalStatus status);

    /** Average approval turnaround time in hours (for SLA reporting). */
    @Query("SELECT AVG(FUNCTION('TIMESTAMPDIFF', HOUR, a.createdAt, a.checkedAt)) " +
           "FROM ApprovalWorkflow a WHERE a.approvalStatus IN ('APPROVED','REJECTED') " +
           "AND a.createdAt >= :fromDate")
    Double averageTurnaroundHours(@Param("fromDate") LocalDateTime fromDate);
}
