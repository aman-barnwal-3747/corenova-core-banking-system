package com.corenova.bank.enums;

/**
 * Transaction lifecycle status.
 * Every state transition is audit-logged.
 */
public enum TransactionStatus {
    INITIATED,          // Transaction request received
    PENDING_APPROVAL,   // Awaiting maker-checker approval (high-value)
    PROCESSING,         // Being processed by payment engine
    SUCCESS,            // Fully settled and confirmed
    FAILED,             // Failed due to insufficient funds / bank error
    REVERSED,           // Successfully reversed / refunded
    CANCELLED,          // Cancelled before processing
    TIMEOUT             // Gateway timeout — requires manual reconciliation
}
