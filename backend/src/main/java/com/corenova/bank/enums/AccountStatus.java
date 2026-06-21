package com.corenova.bank.enums;

/**
 * Lifecycle states for a bank account.
 *
 * State machine:
 *   PENDING_ACTIVATION → ACTIVE → DORMANT → FROZEN → CLOSED
 *                                         ↘ ACTIVE (unfreeze)
 */
public enum AccountStatus {
    PENDING_ACTIVATION, // Account created but KYC not completed
    ACTIVE,             // Fully operational account
    DORMANT,            // No transactions in 12+ months
    FROZEN,             // Temporarily blocked (regulatory/fraud hold)
    CLOSED,             // Permanently closed account
    SUSPENDED           // Suspended pending investigation
}
