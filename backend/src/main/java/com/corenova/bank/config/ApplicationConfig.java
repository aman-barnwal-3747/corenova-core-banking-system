package com.corenova.bank.config;

import com.corenova.bank.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

/**
 * ================================================================
 *  ApplicationConfig – Core Spring Beans Configuration
 *
 *  Registers two critical beans:
 *
 *  1. UserDetailsService
 *     Used by Spring Security to load user by username during
 *     authentication. Fetches from PostgreSQL via UserRepository.
 *     JwtAuthenticationFilter calls this on every request.
 *
 *  2. AuditorAware<String>
 *     Tells Spring Data JPA which username to populate into
 *     BaseEntity.createdBy and BaseEntity.updatedBy fields.
 *     Reads from the active Spring Security context.
 *     Returns "SYSTEM" for scheduled jobs (no authenticated user).
 *
 *  Both beans are defined here (not in SecurityConfig) to avoid
 *  Spring circular dependency issues (SecurityConfig ← UserDetailsService
 *  ← UserRepository — keeping concerns separated).
 * ================================================================
 */
@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    /**
     * Loads user by username for Spring Security authentication chain.
     *
     * Called by:
     *  • DaoAuthenticationProvider during login
     *  • JwtAuthenticationFilter on every authenticated request
     *
     * @throws UsernameNotFoundException if user does not exist or is inactive
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
            .orElseThrow(() ->
                new UsernameNotFoundException("User not found: " + username));
    }

    /**
     * Provides the current authenticated username for JPA audit fields.
     *
     * Spring Data JPA calls getCurrentAuditor() before every INSERT/UPDATE
     * to populate @CreatedBy and @LastModifiedBy fields in BaseEntity.
     *
     * Returns:
     *  • Logged-in username (e.g. "aman.verma") for web requests
     *  • "SYSTEM" for batch jobs / unauthenticated context
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() ||
                "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of("SYSTEM");
            }
            return Optional.of(auth.getName());
        };
    }
}
