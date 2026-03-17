package com.propertize.platform.registry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Eureka Server.
 * Secures the Eureka dashboard and API endpoints.
 *
 * Authentication credentials are configured in application.yml:
 * spring.security.user.name and spring.security.user.password
 *
 * Dashboard access: http://localhost:8761
 * Login with configured credentials
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for Eureka client registration endpoints
                .csrf(AbstractHttpConfigurer::disable)

                // Configure authorization
                .authorizeHttpRequests(auth -> auth
                        // Allow actuator health endpoints without authentication
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/info").permitAll()

                        // Allow human-readable registry info endpoints
                        .requestMatchers("/registry/**").permitAll()

                        // Allow static resources for Eureka dashboard UI
                        .requestMatchers("/eureka/css/**", "/eureka/js/**", "/eureka/fonts/**", "/eureka/images/**")
                        .permitAll()

                        // Allow Eureka client registration endpoints (services need to register)
                        .requestMatchers("/eureka/apps/**").permitAll()
                        .requestMatchers("/eureka/peerreplication/**").permitAll()

                        // Require authentication for dashboard and all other endpoints
                        .anyRequest().authenticated())

                // Enable HTTP Basic authentication for dashboard access
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
