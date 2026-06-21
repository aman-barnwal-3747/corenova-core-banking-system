package com.corenova.bank.controller;

import com.corenova.bank.serviceimpl.DashboardServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * ================================================================
 *  DashboardController – Dashboard KPI & Analytics API
 *
 *  Base path: /api/dashboard
 *
 *  Feeds the CoreNova Bank React dashboard with:
 *    • KPI card metrics (accounts, deposits, transactions)
 *    • 7-day transaction chart data (credit vs debit)
 *    • Recent transactions table
 *    • Account type distribution chart
 *    • System health indicators
 * ================================================================
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard & Analytics", description = "KPI metrics and chart data for the CBS dashboard")
public class DashboardController {

    private final DashboardServiceImpl dashboardService;

    /**
     * Main dashboard data endpoint.
     * Called on every dashboard page load (React useEffect).
     * Response is Redis-cached for 5 minutes.
     */
    @GetMapping
    @Operation(
        summary     = "Get Full Dashboard Data",
        description = "Returns all KPIs, charts, and recent transactions for the main dashboard. " +
                      "Cached in Redis for 5 minutes."
    )
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'AUDITOR')")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboardData());
    }

    /**
     * Lightweight quick-stats for the header/navbar.
     * Refreshed more frequently than full dashboard data.
     */
    @GetMapping("/quick-stats")
    @Operation(summary = "Get Quick Stats", description = "Lightweight stats for the dashboard header bar.")
    @PreAuthorize("hasAnyRole('TELLER', 'MANAGER', 'ADMIN', 'AUDITOR', 'CUSTOMER')")
    public ResponseEntity<Map<String, Object>> getQuickStats() {
        return ResponseEntity.ok(dashboardService.getQuickStats());
    }
}
