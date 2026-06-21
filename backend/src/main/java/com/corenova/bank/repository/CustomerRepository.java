package com.corenova.bank.repository;

import com.corenova.bank.entity.Customer;
import com.corenova.bank.enums.KycStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** CustomerRepository – Customer CIF management queries. */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByCifNumber(String cifNumber);
    Optional<Customer> findByAadhaarNumber(String aadhaarNumber);
    Optional<Customer> findByPanNumber(String panNumber);
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByEmail(String email);

    long countByKycStatus(KycStatus kycStatus);

    /** Full-text search across name, phone, email, CIF. */
    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.lastName)  LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "c.phone LIKE CONCAT('%', :query, '%') OR " +
           "c.email LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "c.cifNumber LIKE CONCAT('%', :query, '%')")
    Page<Customer> searchCustomers(@Param("query") String query, Pageable pageable);
}
