package com.propertize.platform.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway Route Configuration
 *
 * Defines all routes for the API Gateway to route requests to appropriate microservices.
 */
@Configuration
public class GatewayRouteConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()

            // ============================================
            // AUTH SERVICE ROUTES
            // ============================================

            // Authentication routes - no auth required
            .route("auth-service-routes", r -> r
                .path("/api/v1/auth/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway")
                    .circuitBreaker(config -> config
                        .setName("authServiceCircuitBreaker")
                        .setFallbackUri("forward:/fallback/auth-service")))
                .uri("lb://auth-service"))

            // ============================================
            // PROPERTIZE SERVICE ROUTES (Property Management)
            // ============================================

            // Organization onboarding - public routes
            .route("propertize-onboarding", r -> r
                .path("/api/v1/organizations/onboarding/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Properties routes
            .route("propertize-properties", r -> r
                .path("/api/v1/properties/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway")
                    .circuitBreaker(config -> config
                        .setName("propertizeCircuitBreaker")
                        .setFallbackUri("forward:/fallback/propertize")))
                .uri("lb://propertize-service"))

            // Tenants routes
            .route("propertize-tenants", r -> r
                .path("/api/v1/tenants/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Leases routes
            .route("propertize-leases", r -> r
                .path("/api/v1/leases/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Rental Applications routes
            .route("propertize-rental-applications", r -> r
                .path("/api/v1/rental-applications/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Payments routes
            .route("propertize-payments", r -> r
                .path("/api/v1/payments/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Invoices routes
            .route("propertize-invoices", r -> r
                .path("/api/v1/invoices/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Maintenance routes
            .route("propertize-maintenance", r -> r
                .path("/api/v1/maintenance/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Organizations routes
            .route("propertize-organizations", r -> r
                .path("/api/v1/organizations/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Users routes
            .route("propertize-users", r -> r
                .path("/api/v1/users/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Admin routes
            .route("propertize-admin", r -> r
                .path("/api/v1/admin/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Vendors routes
            .route("propertize-vendors", r -> r
                .path("/api/v1/vendors/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Documents routes
            .route("propertize-documents", r -> r
                .path("/api/v1/documents/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Notifications routes
            .route("propertize-notifications", r -> r
                .path("/api/v1/notifications/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Dashboard routes
            .route("propertize-dashboard", r -> r
                .path("/api/v1/dashboard/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Reports routes
            .route("propertize-reports", r -> r
                .path("/api/v1/reports/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Stripe routes
            .route("propertize-stripe", r -> r
                .path("/api/v1/stripe/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Expenses routes
            .route("propertize-expenses", r -> r
                .path("/api/v1/expenses/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // Messages routes
            .route("propertize-messages", r -> r
                .path("/api/v1/messages/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // GraphQL endpoint
            .route("propertize-graphql", r -> r
                .path("/graphql/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://propertize-service"))

            // ============================================
            // EMPLOYECRAFT SERVICE ROUTES (Employee Management)
            // ============================================

            // Employee routes
            .route("employecraft-employees", r -> r
                .path("/api/v1/employees/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway")
                    .circuitBreaker(config -> config
                        .setName("employecraftCircuitBreaker")
                        .setFallbackUri("forward:/fallback/employecraft")))
                .uri("lb://employecraft-service"))

            // Payroll routes
            .route("employecraft-payroll", r -> r
                .path("/api/v1/payroll/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://employecraft-service"))

            // Attendance routes
            .route("employecraft-attendance", r -> r
                .path("/api/v1/attendance/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://employecraft-service"))

            // Timesheet routes
            .route("employecraft-timesheets", r -> r
                .path("/api/v1/timesheets/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://employecraft-service"))

            // Benefits routes
            .route("employecraft-benefits", r -> r
                .path("/api/v1/benefits/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://employecraft-service"))

            // Tax routes
            .route("employecraft-tax", r -> r
                .path("/api/v1/tax/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://employecraft-service"))

            // ============================================
            // WAGECRAFT SERVICE ROUTES (Payroll Processing)
            // ============================================

            // Payroll processing routes
            .route("wagecraft-payroll", r -> r
                .path("/api/v1/payroll/process/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway")
                    .circuitBreaker(config -> config
                        .setName("wagecraftCircuitBreaker")
                        .setFallbackUri("forward:/fallback/wagecraft")))
                .uri("lb://wagecraft-service"))

            // Salary routes
            .route("wagecraft-salaries", r -> r
                .path("/api/v1/salaries/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://wagecraft-service"))

            // Deduction routes
            .route("wagecraft-deductions", r -> r
                .path("/api/v1/deductions/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://wagecraft-service"))

            // Tax calculation routes
            .route("wagecraft-tax-calculations", r -> r
                .path("/api/v1/tax-calculations/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://wagecraft-service"))

            // Payslip routes
            .route("wagecraft-payslips", r -> r
                .path("/api/v1/payslips/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://wagecraft-service"))

            // Wagecraft reports
            .route("wagecraft-reports", r -> r
                .path("/api/v1/payroll/reports/**")
                .filters(f -> f
                    .stripPrefix(0)
                    .addRequestHeader("X-Gateway-Source", "api-gateway"))
                .uri("lb://wagecraft-service"))

            .build();
    }
}
