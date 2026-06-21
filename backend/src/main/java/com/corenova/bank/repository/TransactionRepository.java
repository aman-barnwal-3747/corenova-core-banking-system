package com.corenova.bank.repository;

import com.corenova.bank.entity.Transaction;
import com.corenova.bank.enums.TransactionStatus;
import com.corenova.bank.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * TransactionRepository – Query layer for Transaction records.
 *
 * Transactions are HIGH-VOLUME — always use Pageable for history queries.
 * Never fetch all transactions at once (an account can have millions).
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** Find transaction by unique reference number. */
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    /**
     * Paginated transaction history for an account.
     * Sorted by date descending (newest first) for account statement view.
     */
    Page<Transaction> findByAccount_AccountNumberOrderByTransactionDateDesc(
        String accountNumber, Pageable pageable);

    /** Count today's transactions — for dashboard "Today's Transactions" KPI. */
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.transactionDate >= :startOfDay AND t.transactionStatus = 'SUCCESS'")
    long countTodaysSuccessfulTransactions(@Param("startOfDay") LocalDateTime startOfDay);

    /** Sum today's credit transactions for a branch. */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startOfDay AND t.entryType = 'CREDIT' AND t.transactionStatus = 'SUCCESS'")
    BigDecimal sumTodaysCreditAmount(@Param("startOfDay") LocalDateTime startOfDay);

    /** Sum today's debit transactions — for transaction overview chart. */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.transactionDate >= :startOfDay AND t.entryType = 'DEBIT' AND t.transactionStatus = 'SUCCESS'")
    BigDecimal sumTodaysDebitAmount(@Param("startOfDay") LocalDateTime startOfDay);

    /** Daily credit/debit data for the Transaction Overview line chart (7 days). */
    @Query(value = """
        SELECT DATE(transaction_date) as txn_date,
               SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE 0 END) as credit_total,
               SUM(CASE WHEN entry_type = 'DEBIT'  THEN amount ELSE 0 END) as debit_total
        FROM cbs_transactions
        WHERE transaction_date >= :fromDate AND transaction_status = 'SUCCESS'
        GROUP BY DATE(transaction_date)
        ORDER BY txn_date
        """, nativeQuery = true)
    java.util.List<Object[]> getDailyTransactionSummary(@Param("fromDate") LocalDateTime fromDate);

    /** Recent transactions across all accounts — for dashboard Recent Transactions panel. */
    Page<Transaction> findAllByOrderByTransactionDateDesc(Pageable pageable);

    /** Find pending NEFT transactions for batch settlement job. */
    java.util.List<Transaction> findByTransactionTypeAndTransactionStatus(
        TransactionType type, TransactionStatus status);
}
