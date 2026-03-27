package com.propertize.payment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security config for payment-service.
 * JWT validation is handled by the API Gateway — this service trusts forwarded
 * X-User-* headers.
 * Only Stripe webhook endpoint is open to allow Stripe's callback without auth
 * headers.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Stripe webhooks come from Stripe's servers — no JWT
                        .requestMatchers("/api/v1/webhooks/stripe/**").permitAll()
                        // Org onboarding fees are called before the org is fully created
                        .requestMatchers("/api/v1/organization-application-fees/initiate").permitAll()
                        .requestMatchers("/api/v1/organization-application-fees/*/complete").permitAll()
                        // Health check
                        .requestMatchers("/actuator/health").permitAll()
                        // All other requests must come through gateway (which already validated JWT)
                        .anyRequest().authenticated());
        return http.build();
    }
}
