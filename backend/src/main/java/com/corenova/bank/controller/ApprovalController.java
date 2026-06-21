package com.corenova.bank.controller;

import com.corenova.bank.entity.ApprovalWorkflow;
import com.corenova.bank.serviceimpl.ApprovalWorkflowServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * ================================================================
 *  ApprovalController – Maker-Checker Workflow REST API
 *
 *  Base path: /api/approvals
 *
 *  Endpoints:
 *  ┌─────────────────────────────────────────────────────────────┐
 *  │ POST /create           – Maker: Create approval request    │
 *  │ GET  /pending          – Checker: View pending queue       │
 *  │ GET  /my-requests      – Maker: View own submitted items   │
 *  │ POST /{ref}/approve    – Checker: Approve                  │
 *  │ POST /{ref}/reject     – Checker: Reject with reason       │
 *  │ POST /{ref}/return     – Checker: Return to maker          │
 *  │ GET  /{ref}            – Get approval detail               │
 *  │ GET  /stats            – Pending count + SLA stats         │
 *  └─────────────────────────────────────────────────────────────┘
 * ================================================================
 */
@RestController
@RequestMapping("/approvals")
@RequiredArgsConstructor
@Tag(name = "Maker-Checker Approvals", description = "RBI-mandated maker-checker approval workflow for high-value operations")
public class ApprovalController {

    private final ApprovalWorkflowServiceImpl approvalService;

    @PostMapping("/create")
    @Operation(summary = "Create Approval Request (Maker Action)")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApprovalWorkflow> create(@RequestBody Map<String, Object> body) {
        ApprovalWorkflow approval = approvalService.createApprovalRequest(
            (String) body.get("actionType"),
            (String) body.get("actionDescription"),
            (String) body.get("entityType"),
            (String) body.get("entityRef"),
            (String) body.getOrDefault("requestPayload", "{}"),
            body.get("amountInvolved") != null
                ? new BigDecimal(body.get("amountInvolved").toString()) : null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(approval);
    }

    @GetMapping("/pending")
    @Operation(summary = "Get Pending Checker Queue")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Page<ApprovalWorkflow>> getPending(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(approvalService.getPendingQueue(PageRequest.of(page, size)));
    }

    @GetMapping("/my-requests")
    @Operation(summary = "Get My Submitted Requests (Maker View)")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Page<ApprovalWorkflow>> getMyRequests(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(approvalService.getMyRequests(PageRequest.of(page, size)));
    }

    @PostMapping("/{approvalRef}/approve")
    @Operation(summary = "Approve Request (Checker Action)",
               description = "Checker approves the action. Cannot approve own requests (four-eyes principle).")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApprovalWorkflow> approve(
            @PathVariable String approvalRef,
            @RequestParam(defaultValue = "Approved after verification") String remarks) {
        return ResponseEntity.ok(approvalService.approve(approvalRef, remarks));
    }

    @PostMapping("/{approvalRef}/reject")
    @Operation(summary = "Reject Request (Checker Action)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApprovalWorkflow> reject(
            @PathVariable String approvalRef,
            @RequestParam String rejectionReason) {
        return ResponseEntity.ok(approvalService.reject(approvalRef, rejectionReason));
    }

    @PostMapping("/{approvalRef}/return")
    @Operation(summary = "Return to Maker for Correction")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApprovalWorkflow> returnToMaker(
            @PathVariable String approvalRef,
            @RequestParam String remarks) {
        return ResponseEntity.ok(approvalService.returnToMaker(approvalRef, remarks));
    }

    @GetMapping("/{approvalRef}")
    @Operation(summary = "Get Approval Request Detail")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'AUDITOR')")
    public ResponseEntity<ApprovalWorkflow> getDetail(@PathVariable String approvalRef) {
        return ResponseEntity.ok(approvalService.getByRef(approvalRef));
    }

    @GetMapping("/stats")
    @Operation(summary = "Approval Queue Statistics (Dashboard KPI)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'AUDITOR')")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
            "pendingCount",    approvalService.getPendingCount(),
            "slaWindowHours",  24,
            "queueSummary",    "Pending approvals requiring checker action"
        ));
    }
}
