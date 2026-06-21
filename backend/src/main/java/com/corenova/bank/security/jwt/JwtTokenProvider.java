package com.corenova.bank.security.jwt;

import com.corenova.bank.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * ================================================================
 *  JwtTokenProvider – JWT Token Generator & Validator
 *
 *  Handles all JWT operations for the CoreNova Bank authentication:
 *    1. Token Generation  : Creates signed JWT on successful login
 *    2. Token Validation  : Verifies signature and expiry on every request
 *    3. Claims Extraction : Reads username, role, branch from token
 *
 *  Token Structure (payload claims):
 *    {
 *      "sub"       : "aman.verma",           // username
 *      "role"      : "ROLE_MANAGER",          // user role
 *      "branchCode": "CNB0MAIN001",           // branch
 *      "empId"     : "EMP00123",              // employee ID
 *      "fullName"  : "Aman Verma",            // display name
 *      "iat"       : 1716198600,              // issued at
 *      "exp"       : 1716285000               // expires at (24h)
 *    }
 *
 *  Security: Uses HMAC-SHA256 with a 256-bit secret key.
 *  In production: rotate secret key every 90 days.
 * ================================================================
 */
@Component
@Slf4j
public class JwtTokenProvider {

    /** Secret key loaded from application.yml — must be 256+ bits. */
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    /** Token validity in milliseconds (default: 86400000 = 24 hours). */
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    // ── Token Generation ─────────────────────────────────────────

    /**
     * Generates a signed JWT access token for an authenticated user.
     *
     * Called by AuthService after successful credential validation.
     * The token is returned to the client and must be included in
     * subsequent requests as: "Authorization: Bearer {token}"
     *
     * @param user  Authenticated User entity
     * @return      Signed JWT string
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = buildClaims(user);
        return buildToken(claims, user.getUsername(), jwtExpirationMs);
    }

    /**
     * Generates a refresh token (longer expiry, used to get new access tokens).
     * Refresh tokens are stored server-side in Redis to enable revocation.
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "REFRESH");
        return buildToken(claims, user.getUsername(), refreshExpirationMs);
    }

    /**
     * Builds the custom claims payload embedded in the JWT.
     * Claims allow the frontend to display user info without an API call.
     */
    private Map<String, Object> buildClaims(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role",       user.getRole().name());
        claims.put("branchCode", user.getBranchCode());
        claims.put("empId",      user.getEmployeeId());
        claims.put("fullName",   user.getFullName());
        claims.put("email",      user.getEmail());
        claims.put("type",       "ACCESS");
        return claims;
    }

    /**
     * Core token builder — signs with HMAC-SHA256.
     *
     * @param claims      Payload claims map
     * @param subject     Token subject (username)
     * @param expiryMs    Token lifetime in milliseconds
     */
    private String buildToken(Map<String, Object> claims, String subject, long expiryMs) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
    }

    // ── Token Validation ─────────────────────────────────────────

    /**
     * Validates a JWT token:
     *  1. Signature must match (prevents tampering)
     *  2. Token must not be expired
     *  3. Token must be well-formed
     *
     * Called by JwtAuthenticationFilter on every secured API request.
     *
     * @param token  JWT string from Authorization header
     * @return       true if valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT token unsupported: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("JWT token malformed: {}", ex.getMessage());
        } catch (SecurityException ex) {
            log.warn("JWT signature invalid: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims empty: {}", ex.getMessage());
        }
        return false;
    }

    // ── Claims Extraction ────────────────────────────────────────

    /** Extracts the username (subject) from a valid JWT token. */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /** Extracts the user role from token claims. */
    public String getRoleFromToken(String token) {
        return (String) parseClaims(token).get("role");
    }

    /** Extracts the branch code from token claims. */
    public String getBranchCodeFromToken(String token) {
        return (String) parseClaims(token).get("branchCode");
    }

    /** Checks whether the token has expired (without throwing exception). */
    public boolean isTokenExpired(String token) {
        try {
            return parseClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    // ── Private Helpers ──────────────────────────────────────────

    /**
     * Parses and returns all claims from the JWT payload.
     * Throws JwtException variants if token is invalid.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    /**
     * Derives HMAC SecretKey from the configured secret string.
     * Keys.hmacShaKeyFor() ensures the key meets minimum length requirements.
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }
}
