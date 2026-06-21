package com.corenova.bank.repository;

import com.corenova.bank.entity.Account;
import com.corenova.bank.enums.AccountStatus;
import com.corenova.bank.enums.AccountType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * ================================================================
 *  AccountRepository – JPA Repository for Account Entity
 *
 *  Key query patterns:
 *   1. findByAccountNumber       : Standard lookup (most common)
 *   2. findByAccountNumberWithLock : PESSIMISTIC_WRITE lock for transfers
 *   3. Dashboard summary queries : Aggregates for branch dashboard
 *
 *  The @Lock(PESSIMISTIC_WRITE) on transfer queries is CRITICAL:
 *  It prevents two concurrent transactions from reading the same
 *  balance and both proceeding with a debit (double-spend attack).
 * ================================================================
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /** Standard account lookup by account number. */
    Optional<Account> findByAccountNumber(String accountNumber);

    /**
     * Account lookup WITH pessimistic write lock.
     * Used exclusively in TransactionServiceImpl for fund transfers.
     *
     * PESSIMISTIC_WRITE → issues "SELECT ... FOR UPDATE" in PostgreSQL.
     * Other transactions attempting to lock the same row will WAIT
     * until the current @Transactional method commits or rolls back.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    /** Find all accounts belonging to a customer (by customer ID). */
    List<Account> findByCustomer_CustomerId(Long customerId);

    /** Find all accounts by CIF number (joins Customer entity). */
    @Query("SELECT a FROM Account a WHERE a.customer.cifNumber = :cifNumber")
    List<Account> findByCifNumber(@Param("cifNumber") String cifNumber);

    /** Count total active accounts — used for dashboard KPI card. */
    long countByAccountStatus(AccountStatus status);

    /** Sum all balances by account type — for Account Summary donut chart. */
    @Query("SELECT SUM(a.currentBalance) FROM Account a WHERE a.accountType = :type AND a.accountStatus = 'ACTIVE'")
    BigDecimal sumBalanceByAccountType(@Param("type") AccountType type);

    /** Total deposits (all active accounts combined). */
    @Query("SELECT SUM(a.currentBalance) FROM Account a WHERE a.accountStatus = 'ACTIVE'")
    BigDecimal sumTotalDeposits();

    /** Accounts dormant for more than 365 days — for dormancy processing job. */
    @Query("SELECT a FROM Account a WHERE a.lastTransactionDate < :cutoffDate AND a.accountStatus = 'ACTIVE'")
    List<Account> findDormantAccounts(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);

    /** Checks if an account number is already in use (for new account generation). */
    boolean existsByAccountNumber(String accountNumber);
}
