package com.corenova.bank.entity;

import com.corenova.bank.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * ================================================================
 *  User – System User / Bank Employee Entity
 *
 *  Represents bank employees (Tellers, Managers, Admins) and
 *  self-service customers who access the CBS portal.
 *
 *  Implements Spring Security's UserDetails to integrate seamlessly
 *  with JWT authentication and method-level @PreAuthorize checks.
 *
 *  In Finacle, this maps to the FLEXCUBE User Profile module.
 * ================================================================
 */
@Entity
@Table(
    name = "cbs_users",
    indexes = {
        @Index(name = "idx_user_username", columnList = "username"),
        @Index(name = "idx_user_email",    columnList = "email"),
        @Index(name = "idx_user_emp_id",   columnList = "employee_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    // ── Identity Fields ─────────────────────────────────────────

    /** Bank employee ID (e.g. EMP00123) or customer registration ID. */
    @Column(name = "employee_id", unique = true, length = 20)
    private String employeeId;

    /** Login username — unique across the system. */
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    /** BCrypt-hashed password. Plain text is NEVER stored. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "phone", length = 15)
    private String phone;

    // ── Role & Branch ────────────────────────────────────────────

    /** Single role per user (RBAC — simplest secure model). */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /** Branch code this user is assigned to (e.g. CNB0MAIN001). */
    @Column(name = "branch_code", length = 20)
    private String branchCode;

    @Column(name = "designation", length = 80)
    private String designation;

    // ── Account State ────────────────────────────────────────────

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_locked")
    @Builder.Default
    private Boolean isLocked = false;

    /** After 5 consecutive failed logins, account auto-locks. */
    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    /** Force password reset on next login (e.g. after admin reset). */
    @Column(name = "must_change_password")
    @Builder.Default
    private Boolean mustChangePassword = false;

    // ── Spring Security UserDetails Implementation ───────────────

    /**
     * Maps CoreNova Role enum to Spring Security GrantedAuthority.
     * Used by @PreAuthorize("hasRole('MANAGER')") annotations.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    /** Returns BCrypt hash — Spring Security uses this for authentication. */
    @Override
    public String getPassword() {
        return passwordHash;
    }

    /** Account is non-expired (use isActive flag for business logic). */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** Maps to the isLocked field — locked after repeated failed logins. */
    @Override
    public boolean isAccountNonLocked() {
        return !Boolean.TRUE.equals(isLocked);
    }

    /** Credentials never expire in this implementation (use must_change_password). */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** Only active users can authenticate. */
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(isActive);
    }
}
