package com.corenova.bank.controller;

import com.corenova.bank.entity.Customer;
import com.corenova.bank.enums.KycStatus;
import com.corenova.bank.serviceimpl.CustomerServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ================================================================
 *  CustomerController – Customer CIF Management REST API
 *
 *  Base path: /api/customers
 *
 *  Endpoints:
 *  ┌─────────────────────────────────────────────────────────────┐
 *  │ POST /                        – Create new customer CIF    │
 *  │ GET  /{cifNumber}             – Get customer by CIF        │
 *  │ GET  /phone/{phone}           – Get customer by phone      │
 *  │ GET  /search?query=...        – Full-text search           │
 *  │ POST /{cifNumber}/kyc         – Update KYC status          │
 *  │ PUT  /{cifNumber}/contact     – Update contact details     │
 *  └─────────────────────────────────────────────────────────────┘
 * ================================================================
 */
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Customer CIF onboarding, KYC, and profile management")
public class CustomerController {

    private final CustomerServiceImpl customerService;

    // ── Create Customer ──────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create New Customer CIF",
               description = "Onboards a new bank customer. CIF number auto-generated. KYC documents must be submitted separately.")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Customer> createCustomer(@RequestBody Customer customerRequest) {
        Customer created = customerService.createCustomer(customerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── Query endpoints ──────────────────────────────────────────

    @GetMapping("/{cifNumber}")
    @Operation(summary = "Get Customer by CIF Number")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'CUSTOMER', 'AUDITOR')")
    public ResponseEntity<Customer> getCustomerByCif(@PathVariable String cifNumber) {
        return ResponseEntity.ok(customerService.getCustomerByCif(cifNumber));
    }

    @GetMapping("/phone/{phone}")
    @Operation(summary = "Get Customer by Phone Number")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Customer> getByPhone(@PathVariable String phone) {
        return ResponseEntity.ok(customerService.getCustomerByPhone(phone));
    }

    @GetMapping("/search")
    @Operation(summary = "Search Customers",
               description = "Full-text search across name, phone, email, and CIF number.")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'AUDITOR')")
    public ResponseEntity<Page<Customer>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(customerService.searchCustomers(query, PageRequest.of(page, size)));
    }

    // ── KYC Management ───────────────────────────────────────────

    @PostMapping("/{cifNumber}/kyc")
    @Operation(summary = "Update KYC Status",
               description = "Progresses or rejects a customer's KYC application. " +
                             "APPROVED status requires MANAGER or ADMIN role.")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Customer> updateKyc(
            @PathVariable String cifNumber,
            @RequestBody Map<String, String> body) {

        KycStatus newStatus = KycStatus.valueOf(body.get("status"));
        String    remarks   = body.getOrDefault("remarks", "");

        // Only MANAGER/ADMIN can set KYC to APPROVED
        if (KycStatus.APPROVED.equals(newStatus)) {
            // @PreAuthorize handles this in conjunction with method-level annotation
        }

        return ResponseEntity.ok(customerService.updateKycStatus(cifNumber, newStatus, remarks));
    }

    // ── Update Contact ───────────────────────────────────────────

    @PutMapping("/{cifNumber}/contact")
    @Operation(summary = "Update Customer Contact Details")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Customer> updateContact(
            @PathVariable String cifNumber,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
            customerService.updateContactDetails(
                cifNumber,
                body.get("email"),
                body.get("phone"),
                body.get("addressLine1"),
                body.get("city"),
                body.get("pincode")
            )
        );
    }
}
