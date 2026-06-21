package com.corenova.bank.enums;

/**
 * Maker-Checker approval workflow states.
 *
 * The Maker-Checker pattern is mandatory in RBI-regulated banking:
 *   • Maker  : Teller / Officer initiates an action
 *   • Checker: Manager / Senior Officer approves or rejects
 *
 * Flow: PENDING → APPROVED | REJECTED | RETURNED
 */
public enum ApprovalStatus {
    PENDING,    // Created by maker, awaiting checker action
    APPROVED,   // Checker approved — action proceeds
    REJECTED,   // Checker rejected — action cancelled
    RETURNED,   // Returned to maker for correction
    EXPIRED,    // Auto-expired after SLA breached (e.g. 24 hours)
    CANCELLED   // Maker cancelled before checker reviewed
}
