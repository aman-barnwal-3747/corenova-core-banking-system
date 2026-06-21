package com.corenova.bank.audit;

import com.corenova.bank.entity.AuditLog;
import com.corenova.bank.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * ================================================================
 *  AuditLogService – Async Compliance Audit Logger
 *
 *  Writes audit entries ASYNCHRONOUSLY to avoid blocking the
 *  transaction thread. An audit write failure must NEVER cause
 *  a transaction rollback (hence the separate async call).
 *
 *  In production: consider writing to a separate audit database
 *  or an immutable append-only log store (e.g. AWS CloudTrail).
 *
 *  Usage in service layer:
 *    auditLogService.logAsync("aman.verma", "FUND_TRANSFER",
 *        "ACCOUNT", "1234567890", "Transfer ₹25,000 to Priya",
 *        "SUCCESS", null);
 *
 *  Example log output:
 *    USER [aman.verma] ACTION [FUND_TRANSFER] ON [ACCOUNT:1234567890]
 *    "Transfer ₹25,000 to Priya Sharma" STATUS [SUCCESS] AT [2025-05-20 10:30:00]
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Writes an audit log entry asynchronously.
     *
     * The @Async annotation causes this method to run in a separate
     * thread pool (configured in AsyncConfig), so it never blocks
     * the calling transaction thread.
     *
     * @param performedBy  Username who performed the action
     * @param action       Action code (FUND_TRANSFER, ACCOUNT_OPEN, etc.)
     * @param entityType   Type of entity affected (ACCOUNT, CUSTOMER, etc.)
     * @param entityId     ID of the specific record affected
     * @param description  Human-readable description
     * @param status       SUCCESS or FAILURE
     * @param errorMessage Error details if status is FAILURE
     */
    @Async
    public void logAsync(
            String performedBy,
            String action,
            String entityType,
            String entityId,
            String description,
            String status,
            String errorMessage) {

        try {
            AuditLog auditLog = AuditLog.builder()
                .performedBy(performedBy)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .description(description)
                .status(status)
                .errorMessage(errorMessage)
                .performedAt(LocalDateTime.now())
                .build();

            auditLogRepository.save(auditLog);

            // Also log to application log file for ELK/Splunk ingestion
            log.info("AUDIT | USER [{}] ACTION [{}] ON [{}:{}] STATUS [{}] | {}",
                     performedBy, action, entityType, entityId, status, description);

        } catch (Exception e) {
            // Audit failure MUST NOT propagate to the business transaction.
            // Log the failure for manual audit reconciliation.
            log.error("AUDIT LOG WRITE FAILED | USER [{}] ACTION [{}]: {}",
                      performedBy, action, e.getMessage());
        }
    }
}
