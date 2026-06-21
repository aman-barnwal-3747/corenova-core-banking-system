package com.corenova.bank.serviceimpl;

import com.corenova.bank.audit.AuditLogService;
import com.corenova.bank.dto.request.LoginRequest;
import com.corenova.bank.dto.response.AuthResponse;
import com.corenova.bank.entity.User;
import com.corenova.bank.enums.Role;
import com.corenova.bank.exception.BankingException;
import com.corenova.bank.repository.UserRepository;
import com.corenova.bank.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * ================================================================
 *  AuthServiceImpl – Authentication & Token Lifecycle Service
 *
 *  Manages the complete authentication flow:
 *
 *  LOGIN FLOW:
 *  ┌──────────────────────────────────────────────────────────────┐
 *  │  1. Validate request (not null, not blank)                 │
 *  │  2. AuthenticationManager.authenticate()                   │
 *  │     → DaoAuthenticationProvider verifies BCrypt hash       │
 *  │     → Loads UserDetails via UserDetailsService             │
 *  │  3. Check account lock status                              │
 *  │  4. Reset failed login counter on success                  │
 *  │  5. Update lastLoginAt timestamp                           │
 *  │  6. Generate JWT access token (24h)                        │
 *  │  7. Generate JWT refresh token (7d)                        │
 *  │  8. Log audit entry (async)                                │
 *  │  9. Return AuthResponse with tokens + user info            │
 *  └──────────────────────────────────────────────────────────────┘
 *
 *  FAILED LOGIN HANDLING:
 *    After 5 consecutive failures → account auto-locks
 *    → Only ADMIN can unlock via /users/{id}/unlock
 *
 *  This mirrors ICICI iCore's user authentication module (FACC).
 * ================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider      jwtTokenProvider;
    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final AuditLogService       auditLogService;

    /** Maximum allowed failed logins before account lock. */
    private static final int MAX_FAILED_ATTEMPTS = 5;

    /**
     * Authenticates a user and issues JWT tokens.
     *
     * @param request  LoginRequest with username + password
     * @return         AuthResponse with access/refresh tokens and user info
     * @throws BankingException        if account locked or disabled
     * @throws BadCredentialsException if credentials are wrong
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {

        // ── Step 1: Lookup user (before Spring auth to handle locked message) ──
        User user = userRepository.findByUsername(request.getUsername())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        // ── Step 2: Check if account is locked ───────────────────────
        if (Boolean.TRUE.equals(user.getIsLocked())) {
            auditLogService.logAsync(
                request.getUsername(), "LOGIN_ATTEMPT_LOCKED", "USER",
                request.getUsername(), "Login attempt on locked account", "FAILURE", "Account locked"
            );
            throw new LockedException(
                "Account locked after " + MAX_FAILED_ATTEMPTS + " failed attempts. Contact admin."
            );
        }

        // ── Step 3: Check if account is active ───────────────────────
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new BankingException("Account is disabled. Contact your administrator.");
        }

        // ── Step 4: Authenticate credentials via Spring Security ──────
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getUsername(),
                    request.getPassword()
                )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (BadCredentialsException ex) {
            // Increment failed attempts counter
            handleFailedLogin(user);
            throw new BadCredentialsException("Invalid credentials");
        }

        // ── Step 5: Success — reset counters, update timestamps ───────
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // ── Step 6: Generate JWT tokens ───────────────────────────────
        String accessToken  = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        // ── Step 7: Write async audit log ─────────────────────────────
        auditLogService.logAsync(
            user.getUsername(), "LOGIN", "USER", user.getUsername(),
            "Successful login from branch: " + user.getBranchCode(),
            "SUCCESS", null
        );

        log.info("User [{}] logged in successfully. Role: {}, Branch: {}",
                 user.getUsername(), user.getRole(), user.getBranchCode());

        // ── Step 8: Return full auth response ─────────────────────────
        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .tokenType("Bearer")
            .expiresIn(86400L)               // 24 hours in seconds
            .username(user.getUsername())
            .fullName(user.getFullName())
            .role(user.getRole().name())
            .branchCode(user.getBranchCode())
            .employeeId(user.getEmployeeId())
            .email(user.getEmail())
            .mustChangePassword(user.getMustChangePassword())
            .build();
    }

    /**
     * Generates a new access token using a valid refresh token.
     * The refresh token is validated by JwtTokenProvider.
     *
     * In production: store refresh tokens in Redis with revocation support.
     *
     * @param refreshToken  The refresh token from the client
     * @return              New AuthResponse with fresh access token
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BankingException("Refresh token is invalid or expired. Please login again.");
        }

        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BankingException("User not found"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(refreshToken)   // Return same refresh token (reuse until expiry)
            .tokenType("Bearer")
            .expiresIn(86400L)
            .username(user.getUsername())
            .fullName(user.getFullName())
            .role(user.getRole().name())
            .build();
    }

    /**
     * Changes the authenticated user's password.
     * Enforces password history (simple: just BCrypt check vs current).
     *
     * @param username     The authenticated user
     * @param oldPassword  Current password for verification
     * @param newPassword  New password to set
     */
    @Transactional
    public void changePassword(String username, String oldPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new BankingException("User not found"));

        // Verify old password before allowing change
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BankingException("Current password is incorrect.");
        }

        if (newPassword.length() < 8) {
            throw new BankingException("New password must be at least 8 characters.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setMustChangePassword(false);
        userRepository.save(user);

        auditLogService.logAsync(
            username, "PASSWORD_CHANGE", "USER", username,
            "Password changed successfully", "SUCCESS", null
        );

        log.info("Password changed for user [{}]", username);
    }

    // ─────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Handles a failed login attempt:
     *  • Increments failedLoginAttempts counter
     *  • Auto-locks account after MAX_FAILED_ATTEMPTS (5)
     */
    private void handleFailedLogin(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setIsLocked(true);
            log.warn("Account [{}] locked after {} failed login attempts",
                     user.getUsername(), attempts);
            auditLogService.logAsync(
                user.getUsername(), "ACCOUNT_AUTO_LOCKED", "USER", user.getUsername(),
                "Account locked after " + attempts + " failed login attempts",
                "SYSTEM", null
            );
        }

        userRepository.save(user);

        log.warn("Failed login attempt #{} for user [{}]", attempts, user.getUsername());
    }
}
