package com.corenova.bank.service;

import com.corenova.bank.dto.request.FundTransferRequest;
import com.corenova.bank.dto.request.TransactionRequest;
import com.corenova.bank.dto.response.TransactionResponse;
import com.corenova.bank.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * ================================================================
 *  TransactionService – Core Transaction Engine Interface
 *
 *  Defines all financial transaction operations for CoreNova Bank.
 *  Implementation follows the ACID transaction engine used in
 *  enterprise CBS platforms like Finacle.
 *
 *  Critical Design Principles:
 *  ┌─────────────────────────────────────────────────────────┐
 *  │  ATOMICITY  : Either both debit+credit succeed, or     │
 *  │               both are rolled back (never partial).    │
 *  │  CONSISTENCY: Balance constraints enforced (no         │
 *  │               negative balance, daily limit checks).   │
 *  │  ISOLATION  : Concurrent transfers use pessimistic     │
 *  │               row-level locking (@Lock annotation).    │
 *  │  DURABILITY : Committed to PostgreSQL + audit log.     │
 *  └─────────────────────────────────────────────────────────┘
 * ================================================================
 */
public interface TransactionService {

    /**
     * Initiates an internal fund transfer between two CoreNova accounts.
     * Uses double-entry bookkeeping: one DEBIT + one CREDIT record.
     */
    TransactionResponse initiateInternalTransfer(FundTransferRequest request);

    /**
     * Processes a UPI payment (peer-to-peer via Virtual Payment Address).
     * Simulates NPCI UPI rails with ₹1 Lakh per transaction limit.
     */
    TransactionResponse processUpiPayment(TransactionRequest request);

    /**
     * Processes an NEFT transfer to another bank.
     * NEFT runs in 48 half-hourly settlement batches (8 AM – 7 PM).
     */
    TransactionResponse processNeftTransfer(TransactionRequest request);

    /**
     * Processes an RTGS transfer (minimum ₹2 Lakh, real-time settlement).
     */
    TransactionResponse processRtgsTransfer(TransactionRequest request);

    /**
     * Processes an IMPS transfer (24x7, immediate, up to ₹5 Lakh).
     */
    TransactionResponse processImpsTransfer(TransactionRequest request);

    /**
     * Retrieves paginated transaction history for an account.
     *
     * @param accountNumber  The account whose history is requested
     * @param pageable       Pagination (page, size, sort)
     * @return               Page of transactions (for infinite scroll)
     */
    Page<TransactionResponse> getTransactionHistory(String accountNumber, Pageable pageable);

    /**
     * Initiates a transaction reversal.
     * Creates a new offsetting transaction — never modifies original.
     */
    TransactionResponse reverseTransaction(String referenceNumber, String remarks);

    /**
     * Looks up a transaction by its reference number.
     * Used for customer queries and branch investigations.
     */
    TransactionResponse getByReferenceNumber(String referenceNumber);
}
