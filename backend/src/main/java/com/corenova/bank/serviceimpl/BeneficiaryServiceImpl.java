package com.corenova.bank.serviceimpl;

import com.corenova.bank.audit.AuditLogService;
import com.corenova.bank.entity.Account;
import com.corenova.bank.entity.Beneficiary;
import com.corenova.bank.exception.BankingException;
import com.corenova.bank.repository.AccountRepository;
import com.corenova.bank.repository.BeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ================================================================
 *  BeneficiaryServiceImpl – Saved Payee Management
 *  PDF §8 Payment Gateway Workflow
 *
 *  Banking Rules Enforced:
 *   • 24-hour cooling period after adding any new beneficiary
 *     (RBI fraud prevention guideline — cannot transfer immediately
 *      after adding a new payee via internet banking)
 *   • Duplicate beneficiary prevention (same account + IFSC)
 *   • Soft delete only — beneficiaries are never hard-deleted
 *     (required for audit trail of past transactions)
 *   • Transfer limit per beneficiary (optional fine-grained control)
 *
 *  Cooling Period:
 *   When isVerified = false → transfers BLOCKED
 *   After 24 hours → isVerified = true (set by scheduler)
 *   Branch-added beneficiaries: no cooling period (trusted channel)
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BeneficiaryServiceImpl {

    private final BeneficiaryRepository beneficiaryRepo;
    private final AccountRepository     accountRepo;
    private final AuditLogService       auditLogService;

    // ── ADD BENEFICIARY ──────────────────────────────────────────

    /**
     * Adds a new beneficiary for an account.
     *
     * Flow:
     *  1. Validate account exists and is active
     *  2. Check for duplicate (same account + beneficiary account no)
     *  3. Set 24-hour cooling period (internet banking channel)
     *  4. Save and return
     *
     * @param accountNumber Source account that owns this beneficiary
     * @param type          INTERNAL | EXTERNAL | UPI
     * @param name          Beneficiary full name
     * @param nickname      Friendly label
     * @param benefAccNo    Beneficiary's account number
     * @param ifsc          Beneficiary's IFSC code
     * @param bankName      Beneficiary's bank name
     * @param upiId         UPI VPA (for type=UPI)
     * @param transferLimit Per-transaction limit override
     * @param byBranch      true = no cooling period (branch staff action)
     */
    @Transactional
    @CacheEvict(value = "beneficiaries", key = "#accountNumber")
    public Beneficiary addBeneficiary(
            String accountNumber, String type, String name, String nickname,
            String benefAccNo, String ifsc, String bankName,
            String upiId, BigDecimal transferLimit, boolean byBranch) {

        Account account = accountRepo.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new BankingException("Account not found: " + accountNumber));

        // Duplicate check
        if (benefAccNo != null) {
            beneficiaryRepo.findByAccount_AccountNumberAndBeneficiaryAccountNumber(
                    accountNumber, benefAccNo)
                .ifPresent(existing -> {
                    if (Boolean.TRUE.equals(existing.getIsActive()))
                        throw new BankingException("Beneficiary already exists: " + benefAccNo);
                });
        }
        if (upiId != null) {
            beneficiaryRepo.findByAccount_AccountNumberAndUpiId(accountNumber, upiId)
                .ifPresent(existing -> {
                    if (Boolean.TRUE.equals(existing.getIsActive()))
                        throw new BankingException("UPI beneficiary already exists: " + upiId);
                });
        }

        // 24-hour cooling period (waived for branch-added beneficiaries)
        LocalDateTime coolingEnd = byBranch ? null : LocalDateTime.now().plusHours(24);

        Beneficiary b = Beneficiary.builder()
            .account(account)
            .beneficiaryType(type.toUpperCase())
            .beneficiaryName(name)
            .nickname(nickname)
            .beneficiaryAccountNumber(benefAccNo)
            .beneficiaryIfsc(ifsc)
            .beneficiaryBankName(bankName)
            .upiId(upiId)
            .transferLimit(transferLimit)
            .coolingPeriodEndsAt(coolingEnd)
            .isVerified(byBranch)          // Branch additions are pre-verified
            .isActive(true)
            .build();

        Beneficiary saved = beneficiaryRepo.save(b);

        auditLogService.logAsync(
            getCurrentUsername(), "BENEFICIARY_ADD", "ACCOUNT", accountNumber,
            String.format("Beneficiary added: %s (%s) type=%s cooling=%s",
                name, benefAccNo != null ? benefAccNo : upiId, type,
                byBranch ? "none" : "24h"),
            "SUCCESS", null
        );
        return saved;
    }

    // ── LIST ─────────────────────────────────────────────────────

    @Cacheable(value = "beneficiaries", key = "#accountNumber")
    @Transactional(readOnly = true)
    public List<Beneficiary> getBeneficiaries(String accountNumber) {
        return beneficiaryRepo.findActiveByAccountNumber(accountNumber);
    }

    @Transactional(readOnly = true)
    public List<Beneficiary> getByType(String accountNumber, String type) {
        return beneficiaryRepo.findByAccountNumberAndType(accountNumber, type);
    }

    // ── DELETE (soft) ────────────────────────────────────────────

    @Transactional
    @CacheEvict(value = "beneficiaries", allEntries = true)
    public void deleteBeneficiary(Long beneficiaryId) {
        Beneficiary b = beneficiaryRepo.findById(beneficiaryId)
            .orElseThrow(() -> new BankingException("Beneficiary not found: " + beneficiaryId));
        b.setIsActive(false);
        beneficiaryRepo.save(b);
        auditLogService.logAsync(getCurrentUsername(), "BENEFICIARY_DELETE",
            "BENEFICIARY", beneficiaryId.toString(),
            "Beneficiary soft-deleted: " + b.getBeneficiaryName(), "SUCCESS", null);
    }

    // ── VERIFY (called by scheduler after 24h) ───────────────────
    @Transactional
    public void verifyCooledBeneficiaries() {
        beneficiaryRepo.findAll().stream()
            .filter(b -> !Boolean.TRUE.equals(b.getIsVerified())
                && b.getCoolingPeriodEndsAt() != null
                && LocalDateTime.now().isAfter(b.getCoolingPeriodEndsAt()))
            .forEach(b -> { b.setIsVerified(true); beneficiaryRepo.save(b); });
    }

    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "SYSTEM";
    }
}
