package com.propertize.platform.employecraft.config;

import com.propertize.platform.employecraft.security.filter.TrustedGatewayHeaderFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration for Employecraft
 *
 * Authentication is handled exclusively via the API Gateway.
 * TrustedGatewayHeaderFilter authenticates requests using gateway-forwarded
 * headers.
 * No direct JWT authentication — Auth Service is the single source of truth.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TrustedGatewayHeaderFilter trustedGatewayHeaderFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/api-docs/**", "/webjars/**")
                        .permitAll()

                        // Employee management - Organization level
                        .requestMatchers(HttpMethod.GET, "/api/v1/employees/**")
                        .hasAnyRole("PLATFORM_OVERSIGHT", "ORGANIZATION_OWNER", "ORGANIZATION_ADMIN", "PROPERTY_MANAGER", "ACCOUNTANT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/employees/**")
                        .hasAnyRole("PLATFORM_OVERSIGHT", "ORGANIZATION_OWNER", "ORGANIZATION_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/employees/**")
                        .hasAnyRole("PLATFORM_OVERSIGHT", "ORGANIZATION_OWNER", "ORGANIZATION_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/employees/**")
                        .hasAnyRole("PLATFORM_OVERSIGHT", "ORGANIZATION_OWNER")

                        // Department management
                        .requestMatchers("/api/v1/departments/**")
                        .hasAnyRole("ORGANIZATION_OWNER", "ORGANIZATION_ADMIN")

                        // Payroll - Restricted
                        .requestMatchers("/api/v1/payroll/**")
                        .hasAnyRole("ORGANIZATION_OWNER", "ACCOUNTANT")

                        // Self-service - Any authenticated user
                        .requestMatchers("/api/v1/my/**").authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated())
                // TrustedGatewayHeaderFilter authenticates all gateway requests
                .addFilterBefore(trustedGatewayHeaderFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:8000",
                "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
