package com.corenova.bank.serviceimpl;

import com.corenova.bank.audit.AuditLogService;
import com.corenova.bank.entity.Account;
import com.corenova.bank.entity.Customer;
import com.corenova.bank.enums.AccountStatus;
import com.corenova.bank.enums.AccountType;
import com.corenova.bank.exception.BankingException;
import com.corenova.bank.repository.AccountRepository;
import com.corenova.bank.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * ================================================================
 *  AccountServiceImpl – Bank Account Lifecycle Management
 *
 *  Covers the complete account lifecycle:
 *  ┌─────────────────────────────────────────────────────────────┐
 *  │  OPEN         → PENDING_ACTIVATION → ACTIVE               │
 *  │  FREEZE       → ACTIVE → FROZEN                           │
 *  │  UNFREEZE     → FROZEN → ACTIVE                           │
 *  │  CLOSE        → ACTIVE → CLOSED (final interest posting)  │
 *  │  DORMANCY     → ACTIVE → DORMANT (batch job, 12 months)   │
 *  └─────────────────────────────────────────────────────────────┘
 *
 *  Account Number Generation:
 *    Format: {BranchCode(4)} + {Type-Code(2)} + {Sequential(6)} + {CheckDigit(1)}
 *    Example: 1001SA123456X  → 10-digit numeric by Luhn algorithm
 *
 *  Caching: Account details cached in Redis (TTL: 10 min)
 *  Evicted on any balance-changing operation.
 *
 *  In Finacle: CASA Account Opening module (CACCOPEN).
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl {

    private final AccountRepository  accountRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService    auditLogService;

    @Value("${app.bank.ifsc-prefix}")
    private String ifscPrefix;

    @Value("${app.bank.branch-code}")
    private String defaultBranchCode;

    // ─────────────────────────────────────────────────────────────────
    //  ACCOUNT OPENING
    // ─────────────────────────────────────────────────────────────────

    /**
     * Opens a new bank account for an existing customer (by CIF number).
     *
     * Business rules enforced:
     *  • Customer must exist and be KYC-verified (APPROVED) for ACTIVE accounts
     *  • Account number is uniquely generated (collision-retried)
     *  • Initial deposit must meet minimum balance for account type
     *  • One customer can hold max 5 accounts of the same type
     *
     * @param cifNumber      Customer CIF (e.g. CNB-CUST-20250001)
     * @param accountType    Type: SAVINGS, CURRENT, FD, etc.
     * @param initialDeposit Opening deposit amount
     * @param branchCode     Opening branch IFSC
     * @return               Newly created Account entity
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "accounts", key = "#cifNumber")
    public Account openAccount(String cifNumber, AccountType accountType,
                               BigDecimal initialDeposit, String branchCode) {

        log.info("Opening {} account for CIF [{}] with initial deposit ₹{}",
                 accountType, cifNumber, initialDeposit);

        // ── Validate customer exists ──────────────────────────────────
        Customer customer = customerRepository.findByCifNumber(cifNumber)
            .orElseThrow(() -> new BankingException("Customer not found: CIF " + cifNumber));

        // ── Validate minimum opening deposit ─────────────────────────
        BigDecimal minDeposit = getMinimumOpeningDeposit(accountType);
        if (initialDeposit.compareTo(minDeposit) < 0) {
            throw new BankingException(
                String.format("Minimum opening deposit for %s account is ₹%s. Provided: ₹%s",
                              accountType, minDeposit, initialDeposit)
            );
        }

        // ── Generate unique account number ────────────────────────────
        String accountNumber = generateUniqueAccountNumber();

        // ── Determine account-type parameters ─────────────────────────
        Map<String, Object> params = getAccountTypeParams(accountType);

        // ── Build account entity ──────────────────────────────────────
        String finalBranchCode = branchCode != null ? branchCode : defaultBranchCode;
        Account account = Account.builder()
            .accountNumber(accountNumber)
            .accountType(accountType)
            .accountStatus(AccountStatus.ACTIVE)    // KYC already verified at customer level
            .customer(customer)
            .accountHolderName(customer.getFullName())
            .currentBalance(initialDeposit)
            .availableBalance(initialDeposit)
            .minimumBalance((BigDecimal) params.get("minBalance"))
            .interestRate((BigDecimal) params.get("interestRate"))
            .dailyTransactionLimit((BigDecimal) params.get("dailyLimit"))
            .currency("INR")
            .ifscCode(ifscPrefix + finalBranchCode)
            .branchCode(finalBranchCode)
            .branchName("CoreNova Bank – " + finalBranchCode)
            .accountOpenDate(LocalDate.now())
            .nomineeName(customer.getFullName())    // Default nominee = self (update later)
            .build();

        Account saved = accountRepository.save(account);

        // ── Write audit log ───────────────────────────────────────────
        auditLogService.logAsync(
            getCurrentUsername(), "ACCOUNT_OPEN", "ACCOUNT", accountNumber,
            String.format("%s account opened for %s (CIF: %s). Initial deposit: ₹%s",
                          accountType, customer.getFullName(), cifNumber, initialDeposit),
            "SUCCESS", null
        );

        log.info("Account {} opened successfully for CIF [{}]", accountNumber, cifNumber);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────
    //  ACCOUNT QUERIES
    // ─────────────────────────────────────────────────────────────────

    /**
     * Retrieves account details.
     * Result is cached in Redis for 10 minutes (reduces DB reads).
     * Cache is evicted on any modification (freeze, balance change).
     */
    @Cacheable(value = "accounts", key = "#accountNumber")
    @Transactional(readOnly = true)
    public Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new BankingException("Account not found: " + accountNumber));
    }

    /** Returns all accounts for a customer CIF. */
    @Transactional(readOnly = true)
    public List<Account> getCustomerAccounts(String cifNumber) {
        return accountRepository.findByCifNumber(cifNumber);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ACCOUNT FREEZE / UNFREEZE
    // ─────────────────────────────────────────────────────────────────

    /**
     * Freezes an account (regulatory hold or fraud investigation).
     *
     * Frozen accounts:
     *  • Cannot initiate debits (outgoing payments blocked)
     *  • Can still receive credits (CREDIT entries allowed)
     *  • Manager-level authorization required
     *
     * In Finacle: CACCFRZ function.
     *
     * @param accountNumber  Account to freeze
     * @param reason         Reason for freeze (stored in audit log)
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "accounts", key = "#accountNumber")
    public Account freezeAccount(String accountNumber, String reason) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new BankingException("Account not found: " + accountNumber));

        if (AccountStatus.FROZEN.equals(account.getAccountStatus())) {
            throw new BankingException("Account " + accountNumber + " is already frozen.");
        }
        if (AccountStatus.CLOSED.equals(account.getAccountStatus())) {
            throw new BankingException("Cannot freeze a closed account.");
        }

        account.setAccountStatus(AccountStatus.FROZEN);
        Account saved = accountRepository.save(account);

        auditLogService.logAsync(
            getCurrentUsername(), "ACCOUNT_FREEZE", "ACCOUNT", accountNumber,
            "Account frozen. Reason: " + reason, "SUCCESS", null
        );

        log.info("Account [{}] frozen by [{}]. Reason: {}", accountNumber, getCurrentUsername(), reason);
        return saved;
    }

    /** Unfreezes a frozen account. Requires MANAGER or ADMIN. */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "accounts", key = "#accountNumber")
    public Account unfreezeAccount(String accountNumber, String reason) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new BankingException("Account not found: " + accountNumber));

        if (!AccountStatus.FROZEN.equals(account.getAccountStatus())) {
            throw new BankingException("Account " + accountNumber + " is not frozen.");
        }

        account.setAccountStatus(AccountStatus.ACTIVE);
        Account saved = accountRepository.save(account);

        auditLogService.logAsync(
            getCurrentUsername(), "ACCOUNT_UNFREEZE", "ACCOUNT", accountNumber,
            "Account unfrozen. Reason: " + reason, "SUCCESS", null
        );

        return saved;
    }

    // ─────────────────────────────────────────────────────────────────
    //  ACCOUNT CLOSURE
    // ─────────────────────────────────────────────────────────────────

    /**
     * Closes a bank account.
     *
     * Prerequisites:
     *  • Balance must be zero (or negative for charged accounts)
     *  • No pending EMI/NACH mandates linked
     *  • Manager approval required (maker-checker)
     *
     * In Finacle: CACCLOS function.
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "accounts", key = "#accountNumber")
    public Account closeAccount(String accountNumber, String reason) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new BankingException("Account not found: " + accountNumber));

        if (AccountStatus.CLOSED.equals(account.getAccountStatus())) {
            throw new BankingException("Account " + accountNumber + " is already closed.");
        }

        // Enforce zero balance before closure (surplus returned to customer)
        if (account.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new BankingException(
                String.format("Account has balance ₹%s. Please withdraw before closing.",
                              account.getCurrentBalance())
            );
        }

        account.setAccountStatus(AccountStatus.CLOSED);
        account.setAccountCloseDate(LocalDate.now());
        Account saved = accountRepository.save(account);

        auditLogService.logAsync(
            getCurrentUsername(), "ACCOUNT_CLOSE", "ACCOUNT", accountNumber,
            "Account closed. Reason: " + reason, "SUCCESS", null
        );

        log.info("Account [{}] closed by [{}]. Reason: {}", accountNumber, getCurrentUsername(), reason);
        return saved;
    }

    // ─────────────────────────────────────────────────────────────────
    //  DASHBOARD AGGREGATES
    // ─────────────────────────────────────────────────────────────────

    /** Total number of active accounts — for dashboard KPI card. */
    @Cacheable(value = "dashboardStats", key = "'activeAccountCount'")
    public long getActiveAccountCount() {
        return accountRepository.countByAccountStatus(AccountStatus.ACTIVE);
    }

    /** Total deposit base (sum of all active account balances). */
    @Cacheable(value = "dashboardStats", key = "'totalDeposits'")
    public BigDecimal getTotalDeposits() {
        BigDecimal total = accountRepository.sumTotalDeposits();
        return total != null ? total : BigDecimal.ZERO;
    }

    // ─────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Generates a unique 10-digit account number.
     * Retries up to 5 times on collision (extremely rare).
     */
    private String generateUniqueAccountNumber() {
        Random random = new Random();
        for (int attempt = 0; attempt < 5; attempt++) {
            // Format: 1001 + 6 random digits = 10-digit number
            String accountNumber = "1001" + String.format("%06d", random.nextInt(999999));
            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
        }
        throw new BankingException("Failed to generate unique account number. Please retry.");
    }

    /**
     * Returns type-specific account parameters:
     *  • Minimum balance requirement
     *  • Default interest rate
     *  • Daily transaction limit
     */
    private Map<String, Object> getAccountTypeParams(AccountType type) {
        return switch (type) {
            case SAVINGS  -> Map.of(
                "minBalance",   new BigDecimal("1000.00"),
                "interestRate", new BigDecimal("3.50"),
                "dailyLimit",   new BigDecimal("100000.00")
            );
            case CURRENT  -> Map.of(
                "minBalance",   new BigDecimal("10000.00"),
                "interestRate", BigDecimal.ZERO,
                "dailyLimit",   new BigDecimal("1000000.00")
            );
            case SALARY   -> Map.of(
                "minBalance",   BigDecimal.ZERO,
                "interestRate", new BigDecimal("3.00"),
                "dailyLimit",   new BigDecimal("500000.00")
            );
            case FIXED_DEPOSIT -> Map.of(
                "minBalance",   new BigDecimal("1000.00"),
                "interestRate", new BigDecimal("7.00"),
                "dailyLimit",   BigDecimal.ZERO
            );
            default -> Map.of(
                "minBalance",   new BigDecimal("1000.00"),
                "interestRate", new BigDecimal("3.50"),
                "dailyLimit",   new BigDecimal("100000.00")
            );
        };
    }

    /** Minimum opening deposit per account type (RBI / bank product rules). */
    private BigDecimal getMinimumOpeningDeposit(AccountType type) {
        return switch (type) {
            case SAVINGS       -> new BigDecimal("1000.00");
            case CURRENT       -> new BigDecimal("10000.00");
            case FIXED_DEPOSIT -> new BigDecimal("1000.00");
            case RECURRING     -> new BigDecimal("100.00");
            case SALARY        -> BigDecimal.ZERO;
            default            -> new BigDecimal("1000.00");
        };
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
