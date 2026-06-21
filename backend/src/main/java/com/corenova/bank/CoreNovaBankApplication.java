package com.corenova.bank;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ================================================================
 *  CoreNova Bank – Core Banking System
 *  Main Application Entry Point
 *
 *  Architecture inspired by Finacle / ICICI iCore systems.
 *
 *  Key Capabilities:
 *  ┌──────────────────────────────────────────────────────┐
 *  │  • Customer Lifecycle Management (KYC + Onboarding) │
 *  │  • Multi-account Operations (Savings/Current/FD)    │
 *  │  • Real-time Transaction Engine (ACID compliant)    │
 *  │  • UPI / NEFT / RTGS / IMPS Payment Simulation     │
 *  │  • Maker-Checker Approval Workflows                 │
 *  │  • Role-Based Access Control (RBAC)                 │
 *  │  • Full Audit Trail & Compliance Logging            │
 *  │  • JWT-secured REST APIs with Swagger UI            │
 *  └──────────────────────────────────────────────────────┘
 *
 *  @author  CoreNova Engineering Team
 *  @version 1.0.0
 * ================================================================
 */
@SpringBootApplication
@EnableJpaAuditing                // Auto-populates createdAt / updatedAt via @EntityListeners
@EnableCaching                    // Redis-backed caching for accounts, customer lookups
@EnableAsync                      // Non-blocking audit log writes and notification dispatch
@EnableScheduling                 // Scheduled jobs: EOD reconciliation, interest posting
@OpenAPIDefinition(
    info = @Info(
        title       = "CoreNova Bank – Core Banking API",
        version     = "v1.0",
        description = "Enterprise Core Banking System API inspired by Finacle/iCore. " +
                      "Provides full banking operations: customers, accounts, transactions, payments, " +
                      "loans, approvals, and audit management.",
        contact = @Contact(
            name  = "CoreNova Tech Team",
            email = "api-support@corenova.bank",
            url   = "https://corenova.bank/docs"
        ),
        license = @License(
            name = "Proprietary – CoreNova Bank",
            url  = "https://corenova.bank/license"
        )
    )
)
public class CoreNovaBankApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreNovaBankApplication.class, args);
    }
}
