package com.corenova.bank.controller;

import com.corenova.bank.dto.request.LoginRequest;
import com.corenova.bank.dto.response.AuthResponse;
import com.corenova.bank.serviceimpl.AuthServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ================================================================
 *  AuthController – Authentication & Session Management API
 *
 *  Base path: /api/auth  (PUBLIC — no JWT required)
 *
 *  Endpoints:
 *  ┌──────────────────────────────────────────────────────────────┐
 *  │ POST /auth/login           – Authenticate & get JWT tokens │
 *  │ POST /auth/refresh-token   – Exchange refresh → new access │
 *  │ POST /auth/logout          – Invalidate session (client)   │
 *  │ POST /auth/change-password – Authenticated password change │
 *  └──────────────────────────────────────────────────────────────┘
 *
 *  These endpoints are whitelisted in SecurityConfig (no filter).
 *  All other endpoints require a valid Bearer JWT in the header.
 * ================================================================
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Login, token refresh, and password management APIs")
public class AuthController {

    private final AuthServiceImpl authService;

    /**
     * Authenticates bank employee or customer credentials.
     *
     * Request Body:
     *   { "username": "EMP00123", "password": "SecurePass@123" }
     *
     * Response:
     *   { "accessToken": "eyJ...", "refreshToken": "eyJ...",
     *     "role": "ROLE_TELLER", "fullName": "Aman Verma", ... }
     */
    @PostMapping("/login")
    @Operation(
        summary     = "Login",
        description = "Authenticates user credentials and returns JWT access and refresh tokens. " +
                      "Account locks after 5 consecutive failed attempts."
    )
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Issues a new access token using a valid refresh token.
     * Call this when the frontend receives HTTP 401 on an API call.
     *
     * Request Body:
     *   { "refreshToken": "eyJ..." }
     */
    @PostMapping("/refresh-token")
    @Operation(
        summary     = "Refresh Access Token",
        description = "Exchanges a valid refresh token for a new access token. " +
                      "Refresh tokens expire after 7 days."
    )
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    /**
     * Logout — JWT is stateless, so logout is handled client-side.
     * The client must delete the stored tokens on logout.
     * In production: add token to a Redis deny-list for true revocation.
     */
    @PostMapping("/logout")
    @Operation(
        summary     = "Logout",
        description = "Instructs the client to clear stored JWT tokens. " +
                      "In production, refresh token is added to deny-list."
    )
    public ResponseEntity<Map<String, String>> logout() {
        return ResponseEntity.ok(Map.of(
            "message", "Logged out successfully. Please clear your stored tokens.",
            "status", "SUCCESS"
        ));
    }

    /**
     * Allows an authenticated user to change their own password.
     *
     * Requires:
     *   • Valid Bearer JWT in Authorization header
     *   • Correct current password
     *   • New password (min 8 characters)
     */
    @PostMapping("/change-password")
    @Operation(summary = "Change Password", description = "Allows authenticated users to change their password.")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> body) {

        authService.changePassword(
            userDetails.getUsername(),
            body.get("oldPassword"),
            body.get("newPassword")
        );

        return ResponseEntity.ok(Map.of(
            "message", "Password changed successfully.",
            "status",  "SUCCESS"
        ));
    }

    /**
     * Returns current authenticated user's profile info.
     * Useful for the frontend header "Welcome, Aman Verma" display.
     */
    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile", description = "Returns the authenticated user's profile information.")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
            "username",    userDetails.getUsername(),
            "authorities", userDetails.getAuthorities(),
            "isEnabled",   userDetails.isEnabled()
        ));
    }
}
