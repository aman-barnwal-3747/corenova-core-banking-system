package com.corenova.bank.controller;

import com.corenova.bank.entity.Beneficiary;
import com.corenova.bank.serviceimpl.BeneficiaryServiceImpl;
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
 *  BeneficiaryController – Saved Payee Management API
 *  PDF §8 Payment Gateway Workflow
 *
 *  Base path: /api/beneficiaries
 *
 *  Endpoints:
 *  POST /                        – Add new beneficiary
 *  GET  /account/{accountNumber} – List all beneficiaries
 *  GET  /account/{no}/type/{t}   – Filter by type
 *  DELETE /{id}                  – Soft-delete beneficiary
 * ================================================================
 */
@RestController
@RequestMapping("/beneficiaries")
@RequiredArgsConstructor
@Tag(name = "Beneficiary Management", description = "Saved payee / transfer beneficiary management")
public class BeneficiaryController {

    private final BeneficiaryServiceImpl beneficiaryService;

    @PostMapping
    @Operation(summary = "Add Beneficiary",
               description = "Adds a new beneficiary with 24-hour cooling period (internet banking). " +
                             "Branch-added beneficiaries are immediately verified.")
    @PreAuthorize("hasAnyRole('TELLER','MANAGER','ADMIN','CUSTOMER')")
    public ResponseEntity<Beneficiary> add(@RequestBody Map<String, Object> body) {
        boolean byBranch = Boolean.parseBoolean(body.getOrDefault("byBranch","false").toString());
        Beneficiary b = beneficiaryService.addBeneficiary(
            (String) body.get("accountNumber"),
            (String) body.get("type"),
            (String) body.get("beneficiaryName"),
            (String) body.getOrDefault("nickname", null),
            (String) body.getOrDefault("beneficiaryAccountNumber", null),
            (String) body.getOrDefault("beneficiaryIfsc", null),
            (String) body.getOrDefault("beneficiaryBankName", null),
            (String) body.getOrDefault("upiId", null),
            body.get("transferLimit") != null
                ? new BigDecimal(body.get("transferLimit").toString()) : null,
            byBranch
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(b);
    }

    @GetMapping("/account/{accountNumber}")
    @Operation(summary = "List Beneficiaries for Account")
    @PreAuthorize("hasAnyRole('TELLER','MANAGER','ADMIN','CUSTOMER')")
    public ResponseEntity<List<Beneficiary>> list(@PathVariable String accountNumber) {
        return ResponseEntity.ok(beneficiaryService.getBeneficiaries(accountNumber));
    }

    @GetMapping("/account/{accountNumber}/type/{type}")
    @Operation(summary = "List Beneficiaries by Type (INTERNAL|EXTERNAL|UPI)")
    @PreAuthorize("hasAnyRole('TELLER','MANAGER','ADMIN','CUSTOMER')")
    public ResponseEntity<List<Beneficiary>> listByType(
            @PathVariable String accountNumber, @PathVariable String type) {
        return ResponseEntity.ok(beneficiaryService.getByType(accountNumber, type));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove Beneficiary (Soft Delete)")
    @PreAuthorize("hasAnyRole('TELLER','MANAGER','ADMIN','CUSTOMER')")
    public ResponseEntity<Map<String,String>> delete(@PathVariable Long id) {
        beneficiaryService.deleteBeneficiary(id);
        return ResponseEntity.ok(Map.of("message","Beneficiary removed successfully","status","SUCCESS"));
    }
}
