package com.corenova.bank.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * ================================================================
 *  GlobalExceptionHandler – Centralized Error Response Handler
 *
 *  Catches all exceptions thrown by controllers and services,
 *  translating them into consistent JSON error responses.
 *
 *  Standard error response format:
 *  {
 *    "timestamp"  : "2025-05-20T10:30:00",
 *    "status"     : 400,
 *    "error"      : "Bad Request",
 *    "errorCode"  : "INSUFFICIENT_BALANCE",
 *    "message"    : "Insufficient balance. Available: ₹5,000, Requested: ₹10,000",
 *    "path"       : "/api/transactions/initiate"
 *  }
 *
 *  Benefits:
 *    • No raw stack traces exposed to clients (security)
 *    • Consistent structure for frontend error handling
 *    • All errors logged server-side for audit
 * ================================================================
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles all CoreNova business rule violations.
     * Examples: insufficient balance, frozen account, RTGS limit violation.
     */
    @ExceptionHandler(BankingException.class)
    public ResponseEntity<ErrorResponse> handleBankingException(BankingException ex) {
        log.warn("Banking business rule violation: [{}] {}", ex.getErrorCode(), ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .error("Bad Request")
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .build()
        );
    }

    /**
     * Handles Bean Validation failures (@NotNull, @Size, @DecimalMin, etc.)
     * Returns field-level validation errors for form display on frontend.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName    = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        log.warn("Validation failed: {}", fieldErrors);

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(
            ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(422)
                .error("Validation Failed")
                .errorCode("VALIDATION_ERROR")
                .message("Request validation failed. Check fieldErrors for details.")
                .fieldErrors(fieldErrors)
                .build()
        );
    }

    /**
     * Handles invalid credentials at login.
     * Returns 401 — never expose "wrong password" vs "wrong username" (security).
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
            ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(401)
                .error("Unauthorized")
                .errorCode("INVALID_CREDENTIALS")
                .message("Invalid username or password. Please try again.")
                .build()
        );
    }

    /**
     * Handles account lockout after repeated failed login attempts.
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(LockedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(403)
                .error("Forbidden")
                .errorCode("ACCOUNT_LOCKED")
                .message("Your account has been locked after multiple failed attempts. Contact your administrator.")
                .build()
        );
    }

    /**
     * Handles RBAC violations (@PreAuthorize failures).
     * Returns 403 — user authenticated but not authorized.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
            ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(403)
                .error("Forbidden")
                .errorCode("ACCESS_DENIED")
                .message("You do not have permission to perform this action.")
                .build()
        );
    }

    /**
     * Catch-all for unhandled exceptions.
     * Returns 500 — logs full stack trace server-side, hides internals from client.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled exception: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
            ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(500)
                .error("Internal Server Error")
                .errorCode("SYSTEM_ERROR")
                .message("An unexpected error occurred. Please contact support with reference: " + System.currentTimeMillis())
                .build()
        );
    }

    // ── Inner DTO ────────────────────────────────────────────────────

    /** Standardized error response body. */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private LocalDateTime          timestamp;
        private int                    status;
        private String                 error;
        private String                 errorCode;
        private String                 message;
        private Map<String, String>    fieldErrors; // For validation errors only
    }
}
