package com.corenova.bank.serviceimpl;

import com.corenova.bank.audit.AuditLogService;
import com.corenova.bank.entity.Customer;
import com.corenova.bank.enums.KycStatus;
import com.corenova.bank.exception.BankingException;
import com.corenova.bank.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;

/**
 * ================================================================
 *  CustomerServiceImpl – Customer CIF Management
 *
 *  Manages the complete customer lifecycle in CoreNova Bank:
 *
 *  ONBOARDING FLOW:
 *    1. Branch collects KYC documents + personal info
 *    2. createCustomer() → CIF created (KYC: NOT_SUBMITTED)
 *    3. Documents uploaded (external S3/document service)
 *    4. KYC officer reviews → updateKycStatus(SUBMITTED → UNDER_REVIEW → APPROVED)
 *    5. Account opening enabled once KYC = APPROVED
 *
 *  CIF Number Format: CNB-CUST-{Year}{Sequential-6digits}
 *  Example: CNB-CUST-20250001
 *
 *  In Finacle: Customer Information File (CIF) module.
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl {

    private final CustomerRepository customerRepository;
    private final AuditLogService    auditLogService;

    // ─────────────────────────────────────────────────────────────
    //  CUSTOMER CREATION (Onboarding)
    // ─────────────────────────────────────────────────────────────

    /**
     * Creates a new Customer CIF record.
     *
     * Validation:
     *  • Phone number must be unique (no duplicate customer profiles)
     *  • Aadhaar number must be unique if provided
     *  • PAN must be unique if provided
     *
     * Post-creation:
     *  • CIF number auto-generated (CNB-CUST-{year}{seq})
     *  • KYC status set to NOT_SUBMITTED
     *  • Account opening BLOCKED until KYC = APPROVED
     *
     * @param customer  Partially-filled Customer entity from request
     * @return          Persisted Customer with CIF number assigned
     */
    @Transactional(rollbackFor = Exception.class)
    public Customer createCustomer(Customer customer) {

        log.info("Creating new customer CIF for {} {}", customer.getFirstName(), customer.getLastName());

        // ── Uniqueness checks ─────────────────────────────────────
        if (customerRepository.findByPhone(customer.getPhone()).isPresent()) {
            throw new BankingException("A customer with phone " + customer.getPhone() + " already exists.");
        }
        if (customer.getEmail() != null &&
            customerRepository.findByEmail(customer.getEmail()).isPresent()) {
            throw new BankingException("A customer with email " + customer.getEmail() + " already exists.");
        }
        if (customer.getAadhaarNumber() != null &&
            customerRepository.findByAadhaarNumber(customer.getAadhaarNumber()).isPresent()) {
            throw new BankingException("A customer with this Aadhaar number already exists.");
        }
        if (customer.getPanNumber() != null &&
            customerRepository.findByPanNumber(customer.getPanNumber()).isPresent()) {
            throw new BankingException("A customer with PAN " + customer.getPanNumber() + " already exists.");
        }

        // ── Assign CIF number ─────────────────────────────────────
        customer.setCifNumber(generateCifNumber());
        customer.setKycStatus(KycStatus.NOT_SUBMITTED);
        customer.setIsActive(true);

        Customer saved = customerRepository.save(customer);

        auditLogService.logAsync(
            getCurrentUsername(), "CUSTOMER_CREATE", "CUSTOMER", saved.getCifNumber(),
            String.format("New customer CIF created: %s %s | Phone: %s | Branch: %s",
                customer.getFirstName(), customer.getLastName(),
                customer.getPhone(), customer.getHomeBranchCode()),
            "SUCCESS", null
        );

        log.info("Customer CIF {} created for {} {}", saved.getCifNumber(),
                 saved.getFirstName(), saved.getLastName());
        return saved;
    }

    // ─────────────────────────────────────────────────────────────
    //  KYC MANAGEMENT
    // ─────────────────────────────────────────────────────────────

    /**
     * Updates KYC status for a customer.
     *
     * Allowed transitions (RBI-compliant workflow):
     *   NOT_SUBMITTED → SUBMITTED  (Customer submits docs)
     *   SUBMITTED → UNDER_REVIEW   (Officer picks up for review)
     *   UNDER_REVIEW → APPROVED    (Full KYC completed — account ops enabled)
     *   UNDER_REVIEW → REJECTED    (Docs rejected — resubmission needed)
     *   APPROVED → RE_KYC_DUE      (Periodic re-KYC trigger after 2 years)
     *
     * Only MANAGER and ADMIN can set APPROVED status (enforced in controller).
     */
    @Transactional(rollbackFor = Exception.class)
    public Customer updateKycStatus(String cifNumber, KycStatus newStatus, String remarks) {
        Customer customer = customerRepository.findByCifNumber(cifNumber)
            .orElseThrow(() -> new BankingException("Customer not found: CIF " + cifNumber));

        KycStatus oldStatus = customer.getKycStatus();

        // Validate transition
        validateKycTransition(oldStatus, newStatus);

        customer.setKycStatus(newStatus);
        customer.setKycRemarks(remarks);

        if (KycStatus.APPROVED.equals(newStatus)) {
            customer.setKycVerifiedAt(LocalDateTime.now());
            customer.setKycVerifiedBy(getCurrentUsername());
        }

        Customer saved = customerRepository.save(customer);

        auditLogService.logAsync(
            getCurrentUsername(), "KYC_STATUS_UPDATE", "CUSTOMER", cifNumber,
            String.format("KYC status updated: %s → %s | Remarks: %s", oldStatus, newStatus, remarks),
            "SUCCESS", null
        );

        return saved;
    }

    // ─────────────────────────────────────────────────────────────
    //  QUERY OPERATIONS
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Customer getCustomerByCif(String cifNumber) {
        return customerRepository.findByCifNumber(cifNumber)
            .orElseThrow(() -> new BankingException("Customer not found: CIF " + cifNumber));
    }

    @Transactional(readOnly = true)
    public Customer getCustomerByPhone(String phone) {
        return customerRepository.findByPhone(phone)
            .orElseThrow(() -> new BankingException("Customer not found with phone: " + phone));
    }

    /** Full-text search across name, phone, email, CIF. */
    @Transactional(readOnly = true)
    public Page<Customer> searchCustomers(String query, Pageable pageable) {
        return customerRepository.searchCustomers(query.trim(), pageable);
    }

    // ─────────────────────────────────────────────────────────────
    //  UPDATE OPERATIONS
    // ─────────────────────────────────────────────────────────────

    @Transactional(rollbackFor = Exception.class)
    public Customer updateContactDetails(String cifNumber, String email, String phone,
                                         String addressLine1, String city, String pincode) {
        Customer customer = customerRepository.findByCifNumber(cifNumber)
            .orElseThrow(() -> new BankingException("Customer not found: CIF " + cifNumber));

        if (email    != null) customer.setEmail(email);
        if (phone    != null) customer.setPhone(phone);
        if (addressLine1 != null) customer.setAddressLine1(addressLine1);
        if (city     != null) customer.setCity(city);
        if (pincode  != null) customer.setPincode(pincode);

        Customer saved = customerRepository.save(customer);
        auditLogService.logAsync(getCurrentUsername(), "CUSTOMER_UPDATE", "CUSTOMER", cifNumber,
            "Contact details updated", "SUCCESS", null);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────

    /** Generates a unique CIF number: CNB-CUST-{year}{6-digit-seq}. */
    private String generateCifNumber() {
        long seq  = customerRepository.count() + 1;
        return "CNB-CUST-" + Year.now().getValue() + String.format("%06d", seq);
    }

    /** Validates KYC state-machine transitions. */
    private void validateKycTransition(KycStatus from, KycStatus to) {
        boolean valid = switch (to) {
            case SUBMITTED    -> from == KycStatus.NOT_SUBMITTED || from == KycStatus.REJECTED;
            case UNDER_REVIEW -> from == KycStatus.SUBMITTED;
            case APPROVED     -> from == KycStatus.UNDER_REVIEW;
            case REJECTED     -> from == KycStatus.UNDER_REVIEW || from == KycStatus.SUBMITTED;
            case RE_KYC_DUE   -> from == KycStatus.APPROVED;
            default           -> false;
        };
        if (!valid) {
            throw new BankingException(
                String.format("Invalid KYC transition: %s → %s", from, to));
        }
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
