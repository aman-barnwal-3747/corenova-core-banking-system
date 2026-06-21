package com.corenova.bank.enums;

/** Customer KYC verification state (RBI-mandated). */
public enum KycStatus {
    NOT_SUBMITTED,   // Documents not yet uploaded
    SUBMITTED,       // Docs uploaded, pending officer review
    UNDER_REVIEW,    // Compliance officer reviewing documents
    APPROVED,        // Full KYC completed
    REJECTED,        // Docs rejected — re-submission required
    RE_KYC_DUE,      // Periodic re-KYC required (every 2 years)
    EXPIRED          // KYC lapsed — account restricted
}
