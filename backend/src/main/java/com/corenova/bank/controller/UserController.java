package com.corenova.bank.controller;

import com.corenova.bank.audit.AuditLogService;
import com.corenova.bank.entity.User;
import com.corenova.bank.enums.Role;
import com.corenova.bank.exception.BankingException;
import com.corenova.bank.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ================================================================
 *  UserController – User & Role Management API
 *  PDF §4 Authentication Module: Roles (ADMIN/MANAGER/TELLER/CUSTOMER)
 *
 *  Base path: /api/users
 *  Access: ADMIN only (except /me which is authenticated user)
 *
 *  Endpoints:
 *  GET    /            – List all users (paginated)
 *  POST   /            – Create new bank user
 *  GET    /{id}        – Get user by ID
 *  PUT    /{id}/role   – Change user role
 *  POST   /{id}/lock   – Lock user account
 *  POST   /{id}/unlock – Unlock user account
 *  DELETE /{id}        – Deactivate user (soft delete)
 *  GET    /stats       – User count by role (dashboard)
 * ================================================================
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User & Role Management", description = "Admin: create users, manage roles, lock/unlock accounts")
public class UserController {

    private final UserRepository  userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    // ── List all users ───────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List All Users (paginated)")
    public ResponseEntity<Page<User>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userRepo.findAll(PageRequest.of(page, size)));
    }

    // ── Create user ──────────────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create New Bank User / Employee",
               description = "Creates a new CBS user with assigned role and branch.")
    public ResponseEntity<User> create(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails admin) {

        if (userRepo.existsByUsername(body.get("username")))
            throw new BankingException("Username already exists: " + body.get("username"));
        if (userRepo.existsByEmail(body.get("email")))
            throw new BankingException("Email already registered: " + body.get("email"));

        User user = User.builder()
            .employeeId(body.getOrDefault("employeeId", "EMP" + System.currentTimeMillis()))
            .username(body.get("username"))
            .passwordHash(passwordEncoder.encode(
                body.getOrDefault("password", "Welcome@123")))
            .fullName(body.get("fullName"))
            .email(body.get("email"))
            .phone(body.getOrDefault("phone", null))
            .role(Role.valueOf(body.getOrDefault("role", "ROLE_TELLER")))
            .branchCode(body.getOrDefault("branchCode", "MAIN001"))
            .designation(body.getOrDefault("designation", null))
            .isActive(true)
            .isLocked(false)
            .mustChangePassword(true)   // Force password change on first login
            .build();

        User saved = userRepo.save(user);
        auditLogService.logAsync(admin.getUsername(), "USER_CREATE", "USER",
            saved.getUsername(),
            "User created: " + saved.getFullName() + " role=" + saved.getRole(),
            "SUCCESS", null);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── Get user by ID ───────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get User by ID")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userRepo.findById(id)
            .orElseThrow(() -> new BankingException("User not found: " + id)));
    }

    // ── Change role ──────────────────────────────────────────────
    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change User Role",
               description = "Changes role for a user. Requires ADMIN — role changes are audit-logged.")
    public ResponseEntity<User> changeRole(
            @PathVariable Long id,
            @RequestParam String role,
            @AuthenticationPrincipal UserDetails admin) {
        User user = userRepo.findById(id)
            .orElseThrow(() -> new BankingException("User not found: " + id));
        Role oldRole = user.getRole();
        user.setRole(Role.valueOf(role));
        User saved = userRepo.save(user);
        auditLogService.logAsync(admin.getUsername(), "USER_ROLE_CHANGE", "USER",
            user.getUsername(),
            "Role changed: " + oldRole + " → " + role,
            "SUCCESS", null);
        return ResponseEntity.ok(saved);
    }

    // ── Lock ─────────────────────────────────────────────────────
    @PostMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lock User Account")
    public ResponseEntity<Map<String,String>> lock(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails admin) {
        User user = userRepo.findById(id)
            .orElseThrow(() -> new BankingException("User not found: " + id));
        user.setIsLocked(true);
        userRepo.save(user);
        auditLogService.logAsync(admin.getUsername(), "USER_LOCK", "USER",
            user.getUsername(), "Account locked by admin", "SUCCESS", null);
        return ResponseEntity.ok(Map.of("message","User account locked","status","SUCCESS"));
    }

    // ── Unlock ───────────────────────────────────────────────────
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unlock User Account",
               description = "Resets failed login counter and unlocks user account.")
    public ResponseEntity<Map<String,String>> unlock(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails admin) {
        User user = userRepo.findById(id)
            .orElseThrow(() -> new BankingException("User not found: " + id));
        user.setIsLocked(false);
        user.setFailedLoginAttempts(0);
        userRepo.save(user);
        auditLogService.logAsync(admin.getUsername(), "USER_UNLOCK", "USER",
            user.getUsername(), "Account unlocked by admin", "SUCCESS", null);
        return ResponseEntity.ok(Map.of("message","User account unlocked","status","SUCCESS"));
    }

    // ── Deactivate (soft delete) ─────────────────────────────────
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate User (Soft Delete)")
    public ResponseEntity<Map<String,String>> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails admin) {
        User user = userRepo.findById(id)
            .orElseThrow(() -> new BankingException("User not found: " + id));
        if (admin.getUsername().equals(user.getUsername()))
            throw new BankingException("Cannot deactivate your own account.");
        user.setIsActive(false);
        userRepo.save(user);
        auditLogService.logAsync(admin.getUsername(), "USER_DEACTIVATE", "USER",
            user.getUsername(), "User deactivated", "SUCCESS", null);
        return ResponseEntity.ok(Map.of("message","User deactivated","status","SUCCESS"));
    }

    // ── Stats for dashboard ──────────────────────────────────────
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @Operation(summary = "User Count by Role (Dashboard KPI)")
    public ResponseEntity<Map<String,Object>> stats() {
        return ResponseEntity.ok(Map.of(
            "total",         userRepo.count(),
            "active",        userRepo.countByIsActiveTrue(),
            "admins",        userRepo.countByRole(Role.ROLE_ADMIN),
            "managers",      userRepo.countByRole(Role.ROLE_MANAGER),
            "tellers",       userRepo.countByRole(Role.ROLE_TELLER),
            "customers",     userRepo.countByRole(Role.ROLE_CUSTOMER),
            "auditors",      userRepo.countByRole(Role.ROLE_AUDITOR)
        ));
    }
}
