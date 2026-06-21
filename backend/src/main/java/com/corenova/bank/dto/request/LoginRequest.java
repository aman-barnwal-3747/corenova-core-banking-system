package com.corenova.bank.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * LoginRequest – Credentials submitted at the login screen.
 * Maps to the "Welcome Back!" login form in the CoreNova Bank UI.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    /**
     * Employee ID (e.g. EMP00123) or username.
     * Maps to the "Username / Employee ID" field in the UI.
     */
    @NotBlank(message = "Username or Employee ID is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;
}
