package com.corenova.bank.entity;

import com.corenova.bank.enums.KycStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * ================================================================
 *  Customer – Bank Customer Master Record
 *
 *  The Customer is the central entity in a Core Banking System.
 *  Every account, loan, FD, and payment is linked to a customer.
 *
 *  This entity captures:
 *    • Personal identity (name, DOB, gender)
 *    • KYC documents (Aadhaar, PAN, Passport)
 *    • Contact details (address, email, phone)
 *    • KYC verification status (RBI-mandated)
 *    • Linked bank accounts (one-to-many)
 *
 *  In Finacle CBS, this is the CIF (Customer Information File).
 *  Customer ID format: CNB-CUST-XXXXXXXX
 * ================================================================
 */
@Entity
@Table(
    name = "cbs_customers",
    indexes = {
        @Index(name = "idx_cust_cif",     columnList = "cif_number"),
        @Index(name = "idx_cust_aadhaar", columnList = "aadhaar_number"),
        @Index(name = "idx_cust_pan",     columnList = "pan_number"),
        @Index(name = "idx_cust_phone",   columnList = "phone"),
        @Index(name = "idx_cust_email",   columnList = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "customer_id")
    private Long customerId;

    // ── CIF / Identification ────────────────────────────────────

    /**
     * Customer Information File number — unique bank-assigned ID.
     * Format: CNB-CUST-20250001
     * This is the primary reference used across all banking modules.
     */
    @Column(name = "cif_number", unique = true, nullable = false, length = 20)
    private String cifNumber;

    // ── Personal Information ─────────────────────────────────────

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "middle_name", length = 50)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "nationality", length = 50)
    @Builder.Default
    private String nationality = "Indian";

    @Column(name = "occupation", length = 80)
    private String occupation;

    @Column(name = "annual_income")
    private Double annualIncome;

    // ── Contact Details ──────────────────────────────────────────

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "phone", nullable = false, length = 15)
    private String phone;

    @Column(name = "alternate_phone", length = 15)
    private String alternatePhone;

    // ── KYC Documents (Encrypted in Production) ─────────────────

    /**
     * 12-digit Aadhaar number — masked as XXXX-XXXX-1234 in UI.
     * In production: encrypt with AES-256 + store encrypted value.
     */
    @Column(name = "aadhaar_number", unique = true, length = 12)
    private String aadhaarNumber;

    /**
     * 10-character PAN number (e.g. ABCDE1234F).
     * Mandatory for accounts with annual transactions > ₹2.5 Lakh.
     */
    @Column(name = "pan_number", unique = true, length = 10)
    private String panNumber;

    @Column(name = "passport_number", length = 20)
    private String passportNumber;

    @Column(name = "voter_id", length = 20)
    private String voterId;

    @Column(name = "driving_license", length = 20)
    private String drivingLicense;

    // ── Address ──────────────────────────────────────────────────

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "city", length = 80)
    private String city;

    @Column(name = "state", length = 80)
    private String state;

    @Column(name = "pincode", length = 10)
    private String pincode;

    @Column(name = "country", length = 50)
    @Builder.Default
    private String country = "India";

    // ── KYC Status ───────────────────────────────────────────────

    /**
     * KYC verification status — controls account transaction limits.
     * Full KYC required for accounts with balance > ₹50,000 (RBI).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 20)
    @Builder.Default
    private KycStatus kycStatus = KycStatus.NOT_SUBMITTED;

    @Column(name = "kyc_verified_at")
    private java.time.LocalDateTime kycVerifiedAt;

    @Column(name = "kyc_verified_by", length = 50)
    private String kycVerifiedBy;

    @Column(name = "kyc_remarks", length = 500)
    private String kycRemarks;

    // ── Branch Relationship ──────────────────────────────────────

    /** Home branch where account was opened. */
    @Column(name = "home_branch_code", length = 20)
    private String homeBranchCode;

    @Column(name = "relationship_manager", length = 100)
    private String relationshipManager;

    // ── Customer Segment ─────────────────────────────────────────

    /**
     * Customer segment for product eligibility:
     * RETAIL, HNI (High Net Worth), CORPORATE, NRI, STUDENT, SENIOR_CITIZEN
     */
    @Column(name = "customer_segment", length = 30)
    @Builder.Default
    private String customerSegment = "RETAIL";

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    // ── Linked Accounts ──────────────────────────────────────────

    /**
     * All bank accounts linked to this customer CIF.
     * A customer can have multiple accounts (Savings + Current + FD).
     */
    @OneToMany(mappedBy = "customer",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY)
    @JsonBackReference
    private List<Account> accounts;
    // ── Computed Helper ──────────────────────────────────────────

    /** Returns the full display name: "Aman Verma" */
    public String getFullName() {
        return firstName +
               (middleName != null ? " " + middleName : "") +
               " " + lastName;
    }
}
