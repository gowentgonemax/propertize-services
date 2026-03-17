package com.propertize.platform.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for the API Gateway.
 *
 * IMPORTANT: The API Gateway is now the SINGLE POINT OF AUTHENTICATION.
 *
 * Flow:
 * 1. Client sends request to gateway
 * 2. JwtAuthenticationFilter validates JWT (if required)
 * 3. Gateway adds user context headers (X-Username, X-Roles, etc.)
 * 4. Gateway routes request to downstream service
 * 5. Downstream service trusts gateway headers
 *
 * All endpoints are "permitAll" at Spring Security level because:
 * - JWT validation is handled by JwtAuthenticationFilter (GlobalFilter)
 * - Public vs Protected endpoint logic is in JwtAuthenticationFilter
 * - This config only handles CORS and CSRF
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges
                        // OPTIONS requests for CORS preflight
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()

                        // Health and actuator endpoints
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/health/**").permitAll()

                        // Fallback endpoints
                        .pathMatchers("/fallback/**").permitAll()

                        // Gateway internal endpoints
                        .pathMatchers("/api/v1/gateway/**").permitAll()

                        // Authentication endpoints (handled by gateway AuthController)
                        .pathMatchers("/api/v1/auth/**").permitAll()

                        // Public onboarding endpoints
                        .pathMatchers("/api/v1/organizations/onboarding/**").permitAll()

                        // Public property listings
                        .pathMatchers("/api/v1/properties/public/**").permitAll()

                        // Public rental application submission
                        .pathMatchers("/api/v1/rental-applications/submit").permitAll()
                        .pathMatchers("/api/v1/rental-applications/track/**").permitAll()

                        // API Documentation
                        .pathMatchers("/swagger-ui/**").permitAll()
                        .pathMatchers("/v3/api-docs/**").permitAll()
                        .pathMatchers("/api-docs/**").permitAll()

                        // GraphQL (has internal auth check)
                        .pathMatchers("/graphql").permitAll()
                        .pathMatchers("/graphiql").permitAll()

                        // All other requests - JWT validation handled by JwtAuthenticationFilter
                        // permitAll here because auth is handled by the GlobalFilter
                        .anyExchange().permitAll())
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow all origins for development - restrict in production
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "X-Correlation-Id",
                "X-Request-Id",
                "X-Organization-Id",
                "X-RateLimit-Remaining",
                "X-RateLimit-Limit",
                "X-RateLimit-Reset",
                "Content-Disposition"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
