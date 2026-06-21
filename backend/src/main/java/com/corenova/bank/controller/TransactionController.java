package com.corenova.bank.controller;

import com.corenova.bank.dto.request.FundTransferRequest;
import com.corenova.bank.dto.request.TransactionRequest;
import com.corenova.bank.dto.response.TransactionResponse;
import com.corenova.bank.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * ================================================================
 *  TransactionController – REST API for All Financial Transactions
 *
 *  Base path: /api/transactions
 *
 *  Endpoints:
 *  ┌──────────────────────────────────────────────────────────────┐
 *  │ POST /transfer          – Internal fund transfer            │
 *  │ POST /upi               – UPI payment                      │
 *  │ POST /neft              – NEFT inter-bank transfer         │
 *  │ POST /rtgs              – RTGS high-value transfer         │
 *  │ POST /imps              – IMPS 24x7 transfer               │
 *  │ GET  /{accountNo}/history – Paginated statement            │
 *  │ GET  /{referenceNo}     – Transaction lookup               │
 *  │ POST /{referenceNo}/reverse – Initiate reversal            │
 *  └──────────────────────────────────────────────────────────────┘
 *
 *  All endpoints require JWT authentication.
 *  Reversal and approval require MANAGER or ADMIN role.
 * ================================================================
 */
@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Engine", description = "UPI / NEFT / RTGS / IMPS / Internal Transfer APIs")
public class TransactionController {

    private final TransactionService transactionService;

    // ── Internal Transfer ────────────────────────────────────────

    @PostMapping("/transfer")
    @Operation(
        summary     = "Internal Fund Transfer",
        description = "Transfers funds between two CoreNova Bank accounts. " +
                      "ACID-compliant with pessimistic row locking."
    )
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'CUSTOMER')")
    public ResponseEntity<TransactionResponse> internalTransfer(
            @Valid @RequestBody FundTransferRequest request) {

        TransactionResponse response = transactionService.initiateInternalTransfer(request);
        return ResponseEntity.ok(response);
    }

    // ── UPI Payment ──────────────────────────────────────────────

    @PostMapping("/upi")
    @Operation(summary = "UPI Payment", description = "Process a UPI payment (max ₹1 Lakh per transaction).")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'CUSTOMER')")
    public ResponseEntity<TransactionResponse> upiPayment(
            @Valid @RequestBody TransactionRequest request) {

        return ResponseEntity.ok(transactionService.processUpiPayment(request));
    }

    // ── NEFT Transfer ────────────────────────────────────────────

    @PostMapping("/neft")
    @Operation(
        summary     = "NEFT Transfer",
        description = "Initiates an NEFT inter-bank transfer. " +
                      "Settlement in the next available half-hourly batch."
    )
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'CUSTOMER')")
    public ResponseEntity<TransactionResponse> neftTransfer(
            @Valid @RequestBody TransactionRequest request) {

        return ResponseEntity.ok(transactionService.processNeftTransfer(request));
    }

    // ── RTGS Transfer ────────────────────────────────────────────

    @PostMapping("/rtgs")
    @Operation(
        summary     = "RTGS Transfer",
        description = "RTGS inter-bank transfer (minimum ₹2 Lakh). Real-time gross settlement."
    )
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> rtgsTransfer(
            @Valid @RequestBody TransactionRequest request) {

        return ResponseEntity.ok(transactionService.processRtgsTransfer(request));
    }

    // ── IMPS Transfer ────────────────────────────────────────────

    @PostMapping("/imps")
    @Operation(summary = "IMPS Transfer", description = "24x7 immediate payment (max ₹5 Lakh).")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'CUSTOMER')")
    public ResponseEntity<TransactionResponse> impsTransfer(
            @Valid @RequestBody TransactionRequest request) {

        return ResponseEntity.ok(transactionService.processImpsTransfer(request));
    }

    // ── Transaction History ──────────────────────────────────────

    @GetMapping("/{accountNumber}/history")
    @Operation(
        summary     = "Account Transaction History",
        description = "Returns paginated transaction history for an account. " +
                      "Use page and size query params for pagination."
    )
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'CUSTOMER', 'AUDITOR')")
    public ResponseEntity<Page<TransactionResponse>> getHistory(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountNumber, pageable));
    }

    // ── Transaction Lookup ───────────────────────────────────────

    @GetMapping("/ref/{referenceNumber}")
    @Operation(summary = "Get Transaction by Reference Number")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'CUSTOMER', 'AUDITOR')")
    public ResponseEntity<TransactionResponse> getByReference(
            @PathVariable String referenceNumber) {

        return ResponseEntity.ok(transactionService.getByReferenceNumber(referenceNumber));
    }

    // ── Reversal ─────────────────────────────────────────────────

    @PostMapping("/{referenceNumber}/reverse")
    @Operation(
        summary     = "Reverse a Transaction",
        description = "Initiates a reversal for a successful transaction. " +
                      "Requires MANAGER or ADMIN role (checker approval flow)."
    )
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<TransactionResponse> reverse(
            @PathVariable String referenceNumber,
            @RequestParam(defaultValue = "Manager-initiated reversal") String remarks) {

        return ResponseEntity.ok(transactionService.reverseTransaction(referenceNumber, remarks));
    }
}
