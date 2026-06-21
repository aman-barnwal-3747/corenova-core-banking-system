package com.corenova.bank.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransactionResponse – Returned to client after any transaction.
 * Contains all fields needed to display a transaction receipt.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {

    private Long          transactionId;
    private String        referenceNumber;
    private String        utrNumber;
    private String        transactionType;
    private String        entryType;          // CREDIT or DEBIT
    private BigDecimal    amount;
    private String        currency;
    private String        status;
    private String        fromAccountNumber;
    private String        toAccountNumber;
    private String        narration;
    private String        counterpartyName;
    private String        counterpartyAccountNumber;
    private BigDecimal    balanceAfter;
    private LocalDateTime transactionDate;
    private LocalDateTime settlementDate;
    private String        message;            // User-friendly status message
    private String        failureReason;
}
