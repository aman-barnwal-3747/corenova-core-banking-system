package com.corenova.bank.controller;

import com.corenova.bank.entity.Loan;
import com.corenova.bank.serviceimpl.LoanServiceImpl;
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
import java.util.List;
import java.util.Map;

/**
 * ================================================================
 *  LoanController – Loan Lifecycle REST API
 *
 *  Base path: /api/loans
 *
 *  Endpoints:
 *  ┌─────────────────────────────────────────────────────────────┐
 *  │ POST /apply                – Submit loan application       │
 *  │ POST /{loanNo}/approve     – Approve loan (MANAGER+)       │
 *  │ POST /{loanNo}/reject      – Reject loan (MANAGER+)        │
 *  │ POST /{loanNo}/disburse    – Disburse approved loan        │
 *  │ GET  /{loanNo}             – Get loan details              │
 *  │ GET  /customer/{cif}       – All loans for a customer      │
 *  │ GET  /status/{status}      – Loans by lifecycle status     │
 *  │ GET  /portfolio/summary    – Loan book KPIs (Admin only)   │
 *  │ GET  /emi/calculate        – EMI calculation tool          │
 *  └─────────────────────────────────────────────────────────────┘
 * ================================================================
 */
@RestController
@RequestMapping("/loans")
@RequiredArgsConstructor
@Tag(name = "Loan Management", description = "Loan application, approval, disbursement, and EMI management")
public class LoanController {

    private final LoanServiceImpl loanService;

    // ── Loan Application ─────────────────────────────────────────

    @PostMapping("/apply")
    @Operation(summary = "Apply for Loan",
               description = "Submits a new loan application for a customer. Status begins as APPLIED.")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Loan> applyForLoan(@RequestBody Map<String, Object> body) {
        Loan loan = loanService.applyForLoan(
            (String) body.get("cifNumber"),
            (String) body.get("loanType"),
            new BigDecimal(body.get("requestedAmount").toString()),
            Integer.parseInt(body.get("tenureMonths").toString()),
            new BigDecimal(body.get("interestRate").toString()),
            (String) body.getOrDefault("purpose", "Not specified"),
            (String) body.getOrDefault("repaymentAccountNumber", null)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(loan);
    }

    // ── Approval Workflow ────────────────────────────────────────

    @PostMapping("/{loanNumber}/approve")
    @Operation(summary = "Approve Loan (Checker Action)",
               description = "Approves a loan application. Requires MANAGER or ADMIN role (Checker).")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Loan> approveLoan(
            @PathVariable String loanNumber,
            @RequestParam(defaultValue = "Approved after credit assessment") String remarks) {
        return ResponseEntity.ok(loanService.approveLoan(loanNumber, remarks));
    }

    @PostMapping("/{loanNumber}/reject")
    @Operation(summary = "Reject Loan Application")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Loan> rejectLoan(
            @PathVariable String loanNumber,
            @RequestParam String rejectionReason) {
        return ResponseEntity.ok(loanService.rejectLoan(loanNumber, rejectionReason));
    }

    // ── Disbursement ─────────────────────────────────────────────

    @PostMapping("/{loanNumber}/disburse")
    @Operation(summary = "Disburse Approved Loan",
               description = "Credits the sanctioned amount to borrower's account. MANAGER+ only.")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Loan> disburseLoan(
            @PathVariable String loanNumber,
            @RequestParam String creditAccountNumber) {
        return ResponseEntity.ok(loanService.disburseLoan(loanNumber, creditAccountNumber));
    }

    // ── Query endpoints ──────────────────────────────────────────

    @GetMapping("/{loanNumber}")
    @Operation(summary = "Get Loan Details")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'CUSTOMER', 'AUDITOR')")
    public ResponseEntity<Loan> getLoan(@PathVariable String loanNumber) {
        return ResponseEntity.ok(loanService.getLoanByNumber(loanNumber));
    }

    @GetMapping("/customer/{cifNumber}")
    @Operation(summary = "Get All Loans for Customer")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'CUSTOMER')")
    public ResponseEntity<List<Loan>> getCustomerLoans(@PathVariable String cifNumber) {
        return ResponseEntity.ok(loanService.getCustomerLoans(cifNumber));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get Loans by Status")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'AUDITOR')")
    public ResponseEntity<Page<Loan>> getLoansByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(loanService.getLoansByStatus(status, PageRequest.of(page, size)));
    }

    @GetMapping("/portfolio/summary")
    @Operation(summary = "Loan Portfolio Summary (Dashboard KPI)")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'AUDITOR')")
    public ResponseEntity<Map<String, Object>> getPortfolioSummary() {
        return ResponseEntity.ok(Map.of(
            "totalLoanBook",     loanService.getTotalLoanBook(),
            "activeLoans",       loanService.getLoansByStatus("ACTIVE",    PageRequest.of(0, 1)).getTotalElements(),
            "appliedLoans",      loanService.getLoansByStatus("APPLIED",   PageRequest.of(0, 1)).getTotalElements(),
            "approvedLoans",     loanService.getLoansByStatus("APPROVED",  PageRequest.of(0, 1)).getTotalElements(),
            "disbursedLoans",    loanService.getLoansByStatus("DISBURSED", PageRequest.of(0, 1)).getTotalElements()
        ));
    }

    /**
     * EMI Calculator utility — no authentication required.
     * Used by the customer portal and branch staff for loan planning.
     *
     * Query params: principal, annualRate, tenureMonths
     * Returns: emiAmount, totalInterest, totalPayment
     */
    @GetMapping("/emi/calculate")
    @Operation(summary = "EMI Calculator",
               description = "Calculates EMI using reducing balance method. " +
                             "EMI = P × r × (1+r)^n / ((1+r)^n - 1)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> calculateEmi(
            @RequestParam BigDecimal principal,
            @RequestParam BigDecimal annualRate,
            @RequestParam int tenureMonths) {

        BigDecimal emi          = Loan.calculateEmi(principal, annualRate, tenureMonths);
        BigDecimal totalPayment = emi.multiply(BigDecimal.valueOf(tenureMonths));
        BigDecimal totalInterest = totalPayment.subtract(principal);

        return ResponseEntity.ok(Map.of(
            "principal",      principal,
            "annualRate",     annualRate,
            "tenureMonths",   tenureMonths,
            "emiAmount",      emi,
            "totalInterest",  totalInterest,
            "totalPayment",   totalPayment,
            "loanToValue",    "Calculated based on collateral value"
        ));
    }
}
