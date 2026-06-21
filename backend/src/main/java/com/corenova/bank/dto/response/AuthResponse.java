package com.corenova.bank.dto.response;

import lombok.*;

/**
 * AuthResponse – Returned to client on successful login.
 * The frontend stores accessToken in memory (not localStorage) for security.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String  accessToken;
    private String  refreshToken;
    private String  tokenType;       // Always "Bearer"
    private Long    expiresIn;       // Seconds until access token expires
    private String  username;
    private String  fullName;
    private String  role;
    private String  branchCode;
    private String  branchName;
    private String  employeeId;
    private String  email;
    private Boolean mustChangePassword;
}
