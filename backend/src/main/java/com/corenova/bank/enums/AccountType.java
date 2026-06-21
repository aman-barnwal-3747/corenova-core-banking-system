package com.corenova.bank.enums;

/**
 * Account types supported by CoreNova Bank.
 * Mirrors the product catalogue structure in Finacle CBS.
 */
public enum AccountType {
    SAVINGS,          // Standard savings account (interest-bearing)
    CURRENT,          // Business/current account (overdraft eligible)
    FIXED_DEPOSIT,    // Term deposit with locked-in interest rate
    RECURRING,        // Monthly recurring deposit
    SALARY,           // Salary disbursement account
    NRI               // Non-Resident Indian account
}
