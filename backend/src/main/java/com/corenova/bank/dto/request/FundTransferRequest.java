package com.corenova.bank.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * ================================================================
 *  FundTransferRequest – Internal Transfer Request DTO
 *
 *  Used for transfers between two CoreNova Bank accounts.
 *  Bean Validation (@NotNull, @DecimalMin) enforced before
 *  reaching the service layer.
 * ================================================================
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundTransferRequest {

    /** Source account number (10-digit CoreNova account). */
    @NotBlank(message = "Source account number is required")
    @Size(min = 10, max = 20, message = "Invalid account number format")
    private String fromAccountNumber;

    /** Destination account number. Cannot be same as source. */
    @NotBlank(message = "Destination account number is required")
    @Size(min = 10, max = 20, message = "Invalid account number format")
    private String toAccountNumber;

    /** Transfer amount — must be positive and at least ₹1. */
    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "1.00", message = "Minimum transfer amount is ₹1")
    @DecimalMax(value = "10000000.00", message = "Maximum single transfer is ₹1 Crore")
    private BigDecimal amount;

    /** Customer remarks shown on both account statements. */
    @Size(max = 200, message = "Remarks cannot exceed 200 characters")
    private String remarks;

    /** Transfer purpose code (e.g. FAMILY, BUSINESS, RENT). */
    private String purposeCode;
}
