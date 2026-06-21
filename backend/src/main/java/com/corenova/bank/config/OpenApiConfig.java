package com.corenova.bank.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * ================================================================
 *  OpenApiConfig – Swagger UI Configuration
 *  PDF §15 Advanced Features: Swagger API Documentation
 *
 *  Enables:
 *  1. JWT "Authorize" button in Swagger UI top-right
 *     → Paste: "Bearer eyJhbGciOiJIUzI1NiJ9..."
 *     → All subsequent API calls include Authorization header
 *
 *  2. Multi-server support (local + staging + production)
 *
 *  3. API grouped by tags matching our controller @Tag annotations:
 *     Authentication | Account Management | Transaction Engine |
 *     Customer Management | Loan Management | Maker-Checker |
 *     Dashboard & Analytics | Audit & Compliance
 *
 *  Access: http://localhost:8080/api/swagger-ui.html
 *  JSON:   http://localhost:8080/api/v3/api-docs
 * ================================================================
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "BearerAuth";

    @Value("${app.bank.name:CoreNova Bank}")
    private String bankName;

    /**
     * Configures the root OpenAPI specification.
     *
     * SecurityScheme type HTTP + scheme BEARER means Swagger UI
     * sends: "Authorization: Bearer {token}" automatically
     * after the user clicks "Authorize" and pastes their JWT.
     */
    @Bean
    public OpenAPI coreNovaBankOpenApi() {
        return new OpenAPI()
            // ── API info ─────────────────────────────────────────
            .info(new Info()
                .title("CoreNova Bank – Core Banking System API")
                .version("v1.0.0")
                .description("""
                    **Enterprise Core Banking System** inspired by Finacle/ICICI iCore.
                    
                    ### Authentication
                    1. Call `POST /auth/login` with your credentials
                    2. Copy the `accessToken` from the response
                    3. Click **Authorize** (top right) and enter: `Bearer {token}`
                    4. All secured endpoints will now include the JWT header
                    
                    ### Roles & Access
                    | Role | Access Level |
                    |------|-------------|
                    | ADMIN | Full system access |
                    | MANAGER | Approvals, reports, account management |
                    | TELLER | Transactions, account opening, KYC |
                    | CUSTOMER | Self-service portal |
                    | AUDITOR | Read-only audit and reports |
                    
                    ### Demo Credentials
                    | Username | Password | Role |
                    |----------|----------|------|
                    | admin | Admin@123 | ADMIN |
                    | manager | Manager@123 | MANAGER |
                    | teller | Teller@123 | TELLER |
                    """)
                .contact(new Contact()
                    .name("CoreNova Tech Team")
                    .email("api-support@corenova.bank")
                    .url("https://corenova.bank/docs"))
                .license(new License()
                    .name("Proprietary – CoreNova Bank")
                    .url("https://corenova.bank/license"))
            )

            // ── Server environments ───────────────────────────────
            .servers(List.of(
                new Server().url("http://localhost:8080/api").description("Local Development"),
                new Server().url("https://api-staging.corenova.bank").description("Staging Environment"),
                new Server().url("https://api.corenova.bank").description("Production")
            ))

            // ── JWT Security Scheme ───────────────────────────────
            .components(new Components()
                .addSecuritySchemes(SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Paste your JWT access token here (without the 'Bearer ' prefix)")
                )
            )

            // ── Apply JWT auth to ALL endpoints by default ────────
            .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }
}
