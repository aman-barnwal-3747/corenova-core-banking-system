package com.corenova.bank.security.config;

import com.corenova.bank.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * ================================================================
 *  SecurityConfig – Spring Security Configuration
 *
 *  Configures the complete security posture for CoreNova Bank:
 *
 *  1. STATELESS JWT sessions (no server-side HttpSession)
 *  2. Role-Based Access Control (RBAC) per endpoint:
 *     ┌─────────────────────────────────────────────────────────┐
 *     │ PUBLIC     : /auth/** (login, register)                │
 *     │ ALL ROLES  : /dashboard, /accounts/my-accounts         │
 *     │ TELLER+    : /accounts/open, /transactions/initiate    │
 *     │ MANAGER+   : /approvals/**, /reports/**                │
 *     │ ADMIN ONLY : /users/**, /audit-logs/**                 │
 *     └─────────────────────────────────────────────────────────┘
 *  3. CORS configured for React frontend (localhost:3000 / prod URL)
 *  4. BCrypt password encoding (strength 12 rounds)
 *  5. Method-level security via @EnableMethodSecurity
 *     → Enables @PreAuthorize("hasRole('MANAGER')") on service methods
 *
 *  In Finacle, equivalent = LDAP + Role Matrix configuration.
 * ================================================================
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)  // Enables @PreAuthorize, @PostAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService       userDetailsService;

    // ── Endpoints that do NOT require authentication ─────────────
    private static final String[] PUBLIC_ENDPOINTS = {
        "/auth/login",
        "/auth/register",
        "/auth/refresh-token",
        "/auth/forgot-password",
        "/auth/reset-password",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/actuator/health"
    };

    /**
     * Main security filter chain — defines all HTTP security rules.
     *
     * Key decisions:
     *  - CSRF disabled: JWT tokens provide CSRF protection (stateless)
     *  - SessionCreationPolicy.STATELESS: No HttpSession created
     *  - CORS enabled: Allow React frontend cross-origin requests
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── CSRF ──────────────────────────────────────────────
            // Disabled for REST APIs using JWT (no cookies = no CSRF risk)
            .csrf(AbstractHttpConfigurer::disable)

            // ── CORS ──────────────────────────────────────────────
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // ── Session ───────────────────────────────────────────
            // Stateless: Spring never creates or uses HttpSession
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Authorization Rules ───────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // Public endpoints — no token required
                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                // OPTIONS preflight requests (CORS)
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // Account management — Tellers and above
                .requestMatchers("/accounts/open").hasAnyRole("TELLER", "MANAGER", "ADMIN")
                .requestMatchers("/accounts/freeze/**").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/accounts/close/**").hasAnyRole("MANAGER", "ADMIN")

                // Transactions — all authenticated users
                .requestMatchers("/transactions/initiate").hasAnyRole("TELLER", "MANAGER", "ADMIN", "CUSTOMER")
                .requestMatchers("/transactions/approve/**").hasAnyRole("MANAGER", "ADMIN")

                // Approvals — Managers and Admins only
                .requestMatchers("/approvals/**").hasAnyRole("MANAGER", "ADMIN")

                // Reports — Managers, Admins, Auditors
                .requestMatchers("/reports/**").hasAnyRole("MANAGER", "ADMIN", "AUDITOR")

                // User management — Admin only
                .requestMatchers("/users/**").hasRole("ADMIN")

                // Audit logs — Admin and Auditors only
                .requestMatchers("/audit-logs/**").hasAnyRole("ADMIN", "AUDITOR")

                // KYC approval — Managers and Admins
                .requestMatchers("/customers/kyc/approve/**").hasAnyRole("MANAGER", "ADMIN")

                // All other requests — must be authenticated
                .anyRequest().authenticated()
            )

            // ── Authentication Provider ───────────────────────────
            .authenticationProvider(authenticationProvider())

            // ── JWT Filter ────────────────────────────────────────
            // Runs before Spring's UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS configuration for the React frontend.
     *
     * Development: http://localhost:3000
     * Production:  Update with actual deployed frontend URL
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allowed origins (React dev server + production)
        config.setAllowedOriginPatterns(List.of(
            "http://localhost:3000",    // React dev
            "http://localhost:5173",    // Vite dev
            "https://*.corenova.bank"   // Production
        ));

        // Allowed HTTP methods
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));

        // Allowed headers — must include Authorization for JWT
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "X-Request-ID"));

        // Allow credentials (needed for cookies, if used alongside JWT)
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * DaoAuthenticationProvider — authenticates against PostgreSQL user table.
     * Uses BCrypt to verify passwords (never compares plain text).
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * BCrypt password encoder with strength 12.
     * BCrypt intentionally slow (prevents brute-force attacks).
     * Strength 12 = ~300ms per hash on modern hardware.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** AuthenticationManager — used by AuthService to authenticate credentials. */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
