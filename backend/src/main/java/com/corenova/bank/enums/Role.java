package com.corenova.bank.enums;

/**
 * ================================================================
 *  CoreNova Bank – Enumerations Package
 *
 *  All domain enums used across the banking system.
 *  Organized as inner classes within this file for clarity.
 * ================================================================
 */

// ── User Roles (RBAC) ────────────────────────────────────────────
// Maps to Finacle role hierarchy
public enum Role {
    ROLE_ADMIN,      // System administrator – full access
    ROLE_MANAGER,    // Branch manager – approvals & reports
    ROLE_TELLER,     // Bank teller – transactions & accounts
    ROLE_CUSTOMER,   // Self-service customer portal
    ROLE_AUDITOR     // Read-only audit access
}
