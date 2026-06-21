package com.corenova.bank.repository;

import com.corenova.bank.entity.Beneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * BeneficiaryRepository – Saved payee data access.
 * PDF §8: Payment Gateway – beneficiary management.
 */
@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    /** All active beneficiaries for an account. */
    @Query("SELECT b FROM Beneficiary b WHERE b.account.accountNumber = :accountNumber AND b.isActive = true ORDER BY b.nickname ASC")
    List<Beneficiary> findActiveByAccountNumber(@Param("accountNumber") String accountNumber);

    /** Find by account + beneficiary account number (for duplicate check). */
    Optional<Beneficiary> findByAccount_AccountNumberAndBeneficiaryAccountNumber(
        String accountNumber, String beneficiaryAccountNumber);

    /** Find by UPI ID for a given account. */
    Optional<Beneficiary> findByAccount_AccountNumberAndUpiId(String accountNumber, String upiId);

    /** All beneficiaries of a specific type for an account. */
    @Query("SELECT b FROM Beneficiary b WHERE b.account.accountNumber = :accountNumber AND b.beneficiaryType = :type AND b.isActive = true")
    List<Beneficiary> findByAccountNumberAndType(
        @Param("accountNumber") String accountNumber,
        @Param("type")          String type);
}
