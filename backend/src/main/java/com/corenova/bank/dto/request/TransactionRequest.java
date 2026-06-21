package com.corenova.bank.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * TransactionRequest – Generic Payment Request DTO
 * Used for UPI, NEFT, RTGS, IMPS transactions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionRequest {

    @NotBlank(message = "Source account number is required")
    private String accountNumber;

    /** Target beneficiary account (NEFT/RTGS/IMPS). */
    private String counterpartyAccountNumber;

    /** Beneficiary bank IFSC code (format: XXXX0YYYYYY). */
    private String counterpartyIfsc;

    /** Beneficiary name as per their bank records. */
    private String counterpartyName;

    /** Beneficiary bank name. */
    private String counterpartyBankName;

    /** UPI Virtual Payment Address (e.g. priya@paytm). UPI only. */
    private String counterpartyUpiId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least ₹1")
    private BigDecimal amount;

    @Size(max = 200, message = "Remarks cannot exceed 200 characters")
    private String remarks;
}
