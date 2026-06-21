package com.corenova.bank.enums;

/**
 * Payment channel types supported by CoreNova Bank.
 * Mirrors RBI-regulated payment rails available in Finacle.
 */
public enum TransactionType {
    // ── Intra-bank ─────────────────────────────────────────────
    INTERNAL_TRANSFER,  // Transfer between two CoreNova accounts
    CASH_DEPOSIT,       // Over-the-counter cash deposit
    CASH_WITHDRAWAL,    // Counter cash withdrawal

    // ── RBI Payment Rails ───────────────────────────────────────
    NEFT,               // National Electronic Funds Transfer (batch)
    RTGS,               // Real-Time Gross Settlement (≥ ₹2 Lakh)
    IMPS,               // Immediate Payment Service (24x7, < ₹5 Lakh)
    UPI,                // Unified Payments Interface
    NACH,               // National Automated Clearing House (mandate)

    // ── Card Transactions ───────────────────────────────────────
    DEBIT_CARD,         // POS / e-commerce debit card payment
    CREDIT_CARD,        // Credit card payment

    // ── Banking Services ────────────────────────────────────────
    SALARY_CREDIT,      // Payroll salary credit
    EMI_DEBIT,          // Loan EMI auto-debit
    INTEREST_CREDIT,    // Interest posting on savings/FD
    CHARGES,            // Bank service charges
    REVERSAL,           // Transaction reversal / chargeback
    CHEQUE              // Cheque clearing
}
