package com.corenova.bank.controller;

import com.corenova.bank.entity.AuditLog;
import com.corenova.bank.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * ================================================================
 *  AuditLogController – Compliance Audit Trail REST API
 *
 *  Base path: /api/audit-logs
 *
 *  READ-ONLY endpoints — audit records are NEVER modified or deleted.
 *  Access restricted to ADMIN and AUDITOR roles only.
 *
 *  Used for:
 *   • RBI regulatory audit inspections
 *   • Fraud investigation
 *   • Internal compliance review
 *   • SOX compliance (if applicable)
 * ================================================================
 */
@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit & Compliance", description = "Read-only audit trail for regulatory compliance")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @Operation(summary = "Get All Audit Logs (Paginated)")
    public ResponseEntity<Page<AuditLog>> getAllLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(
            auditLogRepository.findAll(PageRequest.of(page, size))
        );
    }

    @GetMapping("/user/{username}")
    @Operation(summary = "Get Audit Logs by User")
    public ResponseEntity<Page<AuditLog>> getByUser(
            @PathVariable String username,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(
            auditLogRepository.findByPerformedByOrderByPerformedAtDesc(
                username, PageRequest.of(page, size))
        );
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "Get Audit Trail for a Specific Entity")
    public ResponseEntity<Page<AuditLog>> getByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(
            auditLogRepository.findByEntityTypeAndEntityIdOrderByPerformedAtDesc(
                entityType, entityId, PageRequest.of(page, size))
        );
    }

    @GetMapping("/date-range")
    @Operation(summary = "Get Audit Logs in Date Range")
    public ResponseEntity<Page<AuditLog>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(
            auditLogRepository.findByPerformedAtBetweenOrderByPerformedAtDesc(
                from, to, PageRequest.of(page, size))
        );
    }
}
