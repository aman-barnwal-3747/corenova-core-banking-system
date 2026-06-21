package com.corenova.bank.repository;

import com.corenova.bank.entity.Loan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * LoanRepository – Data access for Loan entities.
 *
 * Key query use cases:
 *  • Customer portfolio view (all loans for a CIF)
 *  • NPA classification batch (loans with 3+ overdue EMIs)
 *  • EMI due today (auto-debit scheduling job)
 *  • Loan book size reporting (portfolio analytics)
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByLoanNumber(String loanNumber);

    /** All loans for a given customer CIF. */
    @Query("SELECT l FROM Loan l WHERE l.customer.cifNumber = :cifNumber ORDER BY l.applicationDate DESC")
    List<Loan> findByCustomerCif(@Param("cifNumber") String cifNumber);

    /** Paginated loans by status (ACTIVE, DISBURSED, CLOSED, etc.). */
    Page<Loan> findByLoanStatusOrderByApplicationDateDesc(String loanStatus, Pageable pageable);

    /** All active loans with EMI due on a specific date (for auto-debit job). */
    @Query("SELECT l FROM Loan l WHERE l.nextEmiDate = :dueDate AND l.loanStatus IN ('ACTIVE','DISBURSED')")
    List<Loan> findLoansWithEmiDueOn(@Param("dueDate") LocalDate dueDate);

    /** NPA candidates: active loans with ≥3 overdue EMIs (RBI 90-day rule). */
    @Query("SELECT l FROM Loan l WHERE l.overdueEmis >= 3 AND l.loanStatus = 'ACTIVE'")
    List<Loan> findNpaCandidates();

    /** Total outstanding loan book size (sum of outstanding principals). */
    @Query("SELECT COALESCE(SUM(l.outstandingPrincipal), 0) FROM Loan l WHERE l.loanStatus IN ('ACTIVE','DISBURSED')")
    BigDecimal sumTotalOutstandingPrincipal();

    /** Count active loans by type (for portfolio mix chart). */
    @Query("SELECT l.loanType, COUNT(l) FROM Loan l WHERE l.loanStatus IN ('ACTIVE','DISBURSED') GROUP BY l.loanType")
    List<Object[]> countActiveLoansByType();

    long countByLoanStatus(String loanStatus);
    long countByNpaClassification(String npaClassification);
}
