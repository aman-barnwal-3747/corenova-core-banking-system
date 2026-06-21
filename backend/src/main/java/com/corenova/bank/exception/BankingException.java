package com.corenova.bank.exception;

/**
 * BankingException – Domain-specific unchecked exception.
 *
 * Thrown by service layer for all business rule violations:
 *   • Insufficient balance
 *   • Account frozen/closed
 *   • Daily limit exceeded
 *   • Invalid IFSC / account number
 *   • Regulatory threshold violations
 *
 * Caught by GlobalExceptionHandler → mapped to HTTP 400 Bad Request.
 */
public class BankingException extends RuntimeException {

    private final String errorCode;

    public BankingException(String message) {
        super(message);
        this.errorCode = "BANKING_ERROR";
    }

    public BankingException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "BANKING_ERROR";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
