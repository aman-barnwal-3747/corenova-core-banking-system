package com.corenova.bank.config;

import com.corenova.bank.entity.Account;
import com.corenova.bank.entity.Customer;
import com.corenova.bank.entity.User;
import com.corenova.bank.enums.*;
import com.corenova.bank.repository.AccountRepository;
import com.corenova.bank.repository.CustomerRepository;
import com.corenova.bank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ================================================================
 *  DataInitializer – Demo Data Seeder
 *
 *  Runs on application startup (CommandLineRunner).
 *  Seeds the database with demo users, customers, and accounts
 *  so the system is ready to use immediately after startup.
 *
 *  Demo Users Seeded:
 *  ┌────────────────┬────────────┬─────────────┬─────────────────┐
 *  │ Username       │ Password   │ Role        │ Employee ID     │
 *  ├────────────────┼────────────┼─────────────┼─────────────────┤
 *  │ admin          │ Admin@123  │ ROLE_ADMIN  │ EMP00001        │
 *  │ manager        │ Manager@123│ ROLE_MANAGER│ EMP00002        │
 *  │ teller         │ Teller@123 │ ROLE_TELLER │ EMP00003        │
 *  │ auditor        │ Audit@123  │ ROLE_AUDITOR│ EMP00004        │
 *  └────────────────┴────────────┴─────────────┴─────────────────┘
 *
 *  Demo Customers Seeded:
 *   • Aman Verma  (CIF: CNB-CUST-20250001) — Savings ₹1,25,000
 *   • Priya Sharma (CIF: CNB-CUST-20250002) — Current ₹3,50,000
 *   • Ravi Kumar  (CIF: CNB-CUST-20250003) — Savings ₹75,000
 *
 *  Note: Only runs if data doesn't already exist (idempotent).
 *  Active for all profiles — disable with @Profile("!prod") in production.
 * ================================================================
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository     userRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository  accountRepository;
    private final PasswordEncoder    passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info(" CoreNova Bank – Data Initialization");
        log.info("========================================");

        seedUsers();
        seedCustomers();

        log.info("========================================");
        log.info(" Data initialization complete.");
        log.info(" Dashboard: http://localhost:3000");
        log.info(" Swagger:   http://localhost:8080/api/swagger-ui.html");
        log.info("========================================");
    }

    // ── Seed Bank Users ──────────────────────────────────────────

    private void seedUsers() {
        if (userRepository.count() > 0) {
            log.info("Users already exist — skipping user seed.");
            return;
        }

        log.info("Seeding demo users...");

        userRepository.save(User.builder()
            .employeeId("EMP00001")
            .username("admin")
            .passwordHash(passwordEncoder.encode("Admin@123"))
            .fullName("Arjun Mehra")
            .email("admin@corenova.bank")
            .phone("9000000001")
            .role(Role.ROLE_ADMIN)
            .branchCode("MAIN001")
            .designation("System Administrator")
            .isActive(true)
            .isLocked(false)
            .build());

        userRepository.save(User.builder()
            .employeeId("EMP00002")
            .username("manager")
            .passwordHash(passwordEncoder.encode("Manager@123"))
            .fullName("Sunita Kapoor")
            .email("manager@corenova.bank")
            .phone("9000000002")
            .role(Role.ROLE_MANAGER)
            .branchCode("MAIN001")
            .designation("Branch Manager")
            .isActive(true)
            .isLocked(false)
            .build());

        userRepository.save(User.builder()
            .employeeId("EMP00003")
            .username("teller")
            .passwordHash(passwordEncoder.encode("Teller@123"))
            .fullName("Kiran Bose")
            .email("teller@corenova.bank")
            .phone("9000000003")
            .role(Role.ROLE_TELLER)
            .branchCode("MAIN001")
            .designation("Senior Teller")
            .isActive(true)
            .isLocked(false)
            .build());

        userRepository.save(User.builder()
            .employeeId("EMP00004")
            .username("auditor")
            .passwordHash(passwordEncoder.encode("Audit@123"))
            .fullName("Deepa Nair")
            .email("auditor@corenova.bank")
            .phone("9000000004")
            .role(Role.ROLE_AUDITOR)
            .branchCode("MAIN001")
            .designation("Compliance Auditor")
            .isActive(true)
            .isLocked(false)
            .build());

        log.info("Seeded {} demo users. Login: admin / Admin@123", userRepository.count());
    }

    // ── Seed Customers + Accounts ────────────────────────────────

    private void seedCustomers() {
        if (customerRepository.count() > 0) {
            log.info("Customers already exist — skipping customer seed.");
            return;
        }

        log.info("Seeding demo customers and accounts...");

        // ── Customer 1: Aman Verma ────────────────────────────────
        Customer aman = customerRepository.save(Customer.builder()
            .cifNumber("CNB-CUST-20250001")
            .firstName("Aman")
            .lastName("Verma")
            .dateOfBirth(LocalDate.of(1988, 4, 15))
            .gender("Male")
            .email("aman.verma@email.com")
            .phone("9876543210")
            .aadhaarNumber("123456789012")
            .panNumber("ABCPV1234D")
            .addressLine1("12, Green Park Colony")
            .city("New Delhi")
            .state("Delhi")
            .pincode("110016")
            .kycStatus(KycStatus.APPROVED)
            .kycVerifiedAt(java.time.LocalDateTime.now().minusDays(30))
            .kycVerifiedBy("manager")
            .homeBranchCode("MAIN001")
            .customerSegment("RETAIL")
            .occupation("Software Engineer")
            .annualIncome(1200000.0)
            .isActive(true)
            .build());

        accountRepository.save(Account.builder()
            .accountNumber("1001000001")
            .accountType(AccountType.SAVINGS)
            .accountStatus(AccountStatus.ACTIVE)
            .customer(aman)
            .accountHolderName("Aman Verma")
            .currentBalance(new BigDecimal("125000.00"))
            .availableBalance(new BigDecimal("125000.00"))
            .minimumBalance(new BigDecimal("1000.00"))
            .interestRate(new BigDecimal("3.50"))
            .dailyTransactionLimit(new BigDecimal("100000.00"))
            .currency("INR")
            .ifscCode("CNB0MAIN001")
            .branchCode("MAIN001")
            .branchName("CoreNova Bank – Main Branch")
            .accountOpenDate(LocalDate.of(2023, 1, 10))
            .lastTransactionDate(java.time.LocalDateTime.now().minusHours(2))
            .build());

        // ── Customer 2: Priya Sharma ──────────────────────────────
        Customer priya = customerRepository.save(Customer.builder()
            .cifNumber("CNB-CUST-20250002")
            .firstName("Priya")
            .lastName("Sharma")
            .dateOfBirth(LocalDate.of(1992, 8, 20))
            .gender("Female")
            .email("priya.sharma@email.com")
            .phone("9876543211")
            .aadhaarNumber("234567890123")
            .panNumber("BCQPS5678E")
            .addressLine1("45, Bandra West")
            .city("Mumbai")
            .state("Maharashtra")
            .pincode("400050")
            .kycStatus(KycStatus.APPROVED)
            .kycVerifiedAt(java.time.LocalDateTime.now().minusDays(60))
            .kycVerifiedBy("manager")
            .homeBranchCode("MAIN001")
            .customerSegment("HNI")
            .occupation("Business Owner")
            .annualIncome(5000000.0)
            .isActive(true)
            .build());

        accountRepository.save(Account.builder()
            .accountNumber("1001000002")
            .accountType(AccountType.CURRENT)
            .accountStatus(AccountStatus.ACTIVE)
            .customer(priya)
            .accountHolderName("Priya Sharma")
            .currentBalance(new BigDecimal("350000.00"))
            .availableBalance(new BigDecimal("350000.00"))
            .minimumBalance(new BigDecimal("10000.00"))
            .interestRate(BigDecimal.ZERO)
            .dailyTransactionLimit(new BigDecimal("1000000.00"))
            .currency("INR")
            .ifscCode("CNB0MAIN001")
            .branchCode("MAIN001")
            .branchName("CoreNova Bank – Main Branch")
            .accountOpenDate(LocalDate.of(2022, 6, 15))
            .lastTransactionDate(java.time.LocalDateTime.now().minusHours(5))
            .build());

        // ── Customer 3: Ravi Kumar ────────────────────────────────
        Customer ravi = customerRepository.save(Customer.builder()
            .cifNumber("CNB-CUST-20250003")
            .firstName("Ravi")
            .lastName("Kumar")
            .dateOfBirth(LocalDate.of(1975, 12, 5))
            .gender("Male")
            .email("ravi.kumar@email.com")
            .phone("9876543212")
            .aadhaarNumber("345678901234")
            .panNumber("CDRRK9012F")
            .addressLine1("78, Koramangala 5th Block")
            .city("Bengaluru")
            .state("Karnataka")
            .pincode("560095")
            .kycStatus(KycStatus.APPROVED)
            .kycVerifiedAt(java.time.LocalDateTime.now().minusDays(90))
            .kycVerifiedBy("manager")
            .homeBranchCode("MAIN001")
            .customerSegment("RETAIL")
            .occupation("Government Employee")
            .annualIncome(800000.0)
            .isActive(true)
            .build());

        accountRepository.save(Account.builder()
            .accountNumber("1001000003")
            .accountType(AccountType.SALARY)
            .accountStatus(AccountStatus.ACTIVE)
            .customer(ravi)
            .accountHolderName("Ravi Kumar")
            .currentBalance(new BigDecimal("75000.00"))
            .availableBalance(new BigDecimal("75000.00"))
            .minimumBalance(BigDecimal.ZERO)
            .interestRate(new BigDecimal("3.00"))
            .dailyTransactionLimit(new BigDecimal("500000.00"))
            .currency("INR")
            .ifscCode("CNB0MAIN001")
            .branchCode("MAIN001")
            .branchName("CoreNova Bank – Main Branch")
            .accountOpenDate(LocalDate.of(2021, 3, 1))
            .lastTransactionDate(java.time.LocalDateTime.now().minusDays(1))
            .build());

        log.info("Seeded {} customers with accounts.", customerRepository.count());
        log.info("Demo accounts: 1001000001 (Savings), 1001000002 (Current), 1001000003 (Salary)");
    }
}
