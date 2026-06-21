package com.corenova.bank.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * ================================================================
 *  JwtAuthenticationFilter – Per-Request JWT Interceptor
 *
 *  Runs ONCE per HTTP request (extends OncePerRequestFilter).
 *  Intercepts every API call and validates the JWT Bearer token.
 *
 *  Filter pipeline:
 *    HTTP Request
 *      ↓
 *    Extract "Authorization: Bearer {token}" header
 *      ↓
 *    Validate JWT signature and expiry (JwtTokenProvider)
 *      ↓
 *    Load UserDetails from database (UserDetailsService)
 *      ↓
 *    Set Authentication in Spring SecurityContext
 *      ↓
 *    Continue to Controller (@PreAuthorize checks run here)
 *
 *  If the token is missing or invalid:
 *    → SecurityContext remains null
 *    → Spring Security returns HTTP 401 Unauthorized
 *
 *  Registered in SecurityConfig's filter chain via:
 *    .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
 * ================================================================
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider   jwtTokenProvider;
    private final UserDetailsService  userDetailsService;

    /**
     * Core filter logic — runs on every secured request.
     *
     * @param request     Incoming HTTP request
     * @param response    Outgoing HTTP response
     * @param filterChain Spring Security filter chain
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         filterChain) throws ServletException, IOException {

        try {
            // ── Step 1: Extract JWT from Authorization header ────────
            String jwt = extractJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {

                // ── Step 2: Validate token signature and expiry ──────
                if (jwtTokenProvider.validateToken(jwt)) {

                    // ── Step 3: Get username from token claims ───────
                    String username = jwtTokenProvider.getUsernameFromToken(jwt);

                    // ── Step 4: Load full user details from database ─
                    // Ensures the user account is still active and not locked.
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    // ── Step 5: Create authenticated principal ───────
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,                          // credentials null after auth
                            userDetails.getAuthorities()   // roles for @PreAuthorize
                        );

                    // Attach request details (IP address, session ID) to auth object.
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // ── Step 6: Set auth in SecurityContext ──────────
                    // This makes the current user available via:
                    // SecurityContextHolder.getContext().getAuthentication()
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Authenticated user [{}] for URI [{}]",
                              username, request.getRequestURI());
                } else {
                    log.debug("Invalid JWT token for URI: {}", request.getRequestURI());
                }
            }
        } catch (Exception ex) {
            // Don't re-throw — let Spring Security handle the 401 response.
            // Logging here prevents silent failures in audit trail.
            log.error("JWT filter error for URI [{}]: {}",
                      request.getRequestURI(), ex.getMessage());
        }

        // ── Always continue the filter chain ────────────────────────
        // Even if auth fails — Spring Security will reject at the controller layer.
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT string from the HTTP Authorization header.
     *
     * Expected format: "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJz..."
     *
     * @param request  HTTP request
     * @return JWT token string, or null if header is missing/malformed
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Strip "Bearer " prefix (7 characters) to get the raw token
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * Skip JWT processing for public endpoints (login, swagger, actuator).
     * Returns true to skip this filter entirely for those paths.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth/")        ||  // Login, register
               path.startsWith("/v3/api-docs")  ||  // OpenAPI spec
               path.startsWith("/swagger-ui")   ||  // Swagger UI
               path.startsWith("/actuator/health"); // Health check
    }
}
