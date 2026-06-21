package com.corenova.bank.controller;

import com.corenova.bank.entity.Account;
import com.corenova.bank.enums.AccountType;
import com.corenova.bank.serviceimpl.AccountServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * ================================================================
 *  AccountController – Account Lifecycle REST API
 *
 *  Base path: /api/accounts
 *
 *  Endpoints:
 *  ┌──────────────────────────────────────────────────────────────┐
 *  │ POST /open                  – Open a new account           │
 *  │ GET  /{accountNumber}       – Get account details          │
 *  │ GET  /customer/{cif}        – All accounts for a customer  │
 *  │ POST /{accountNumber}/freeze   – Freeze account            │
 *  │ POST /{accountNumber}/unfreeze – Unfreeze account          │
 *  │ POST /{accountNumber}/close    – Close account             │
 *  └──────────────────────────────────────────────────────────────┘
 * ================================================================
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "Account lifecycle: open, query, freeze, close")
public class AccountController {

    private final AccountServiceImpl accountService;

    @PostMapping("/open")
    @Operation(summary = "Open New Account",
               description = "Opens a bank account for an existing customer (by CIF number).")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Account> openAccount(@RequestBody Map<String, Object> body) {
        String      cifNumber      = (String) body.get("cifNumber");
        AccountType type           = AccountType.valueOf((String) body.get("accountType"));
        BigDecimal  initialDeposit = new BigDecimal(body.get("initialDeposit").toString());
        String      branchCode     = (String) body.getOrDefault("branchCode", null);

        Account account = accountService.openAccount(cifNumber, type, initialDeposit, branchCode);
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get Account Details")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'CUSTOMER', 'AUDITOR')")
    public ResponseEntity<Account> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    @GetMapping("/customer/{cifNumber}")
    @Operation(summary = "Get All Accounts for Customer CIF")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'CUSTOMER')")
    public ResponseEntity<List<Account>> getCustomerAccounts(@PathVariable String cifNumber) {
        return ResponseEntity.ok(accountService.getCustomerAccounts(cifNumber));
    }

    @PostMapping("/{accountNumber}/freeze")
    @Operation(summary = "Freeze Account", description = "Places a regulatory or fraud hold on the account.")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Account> freezeAccount(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "Regulatory hold") String reason) {
        return ResponseEntity.ok(accountService.freezeAccount(accountNumber, reason));
    }

    @PostMapping("/{accountNumber}/unfreeze")
    @Operation(summary = "Unfreeze Account")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Account> unfreezeAccount(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "Hold lifted") String reason) {
        return ResponseEntity.ok(accountService.unfreezeAccount(accountNumber, reason));
    }

    @PostMapping("/{accountNumber}/close")
    @Operation(summary = "Close Account",
               description = "Permanently closes an account. Balance must be zero.")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Account> closeAccount(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "Customer request") String reason) {
        return ResponseEntity.ok(accountService.closeAccount(accountNumber, reason));
    }
}
