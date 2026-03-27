package com.propertize.platform.auth.config;

import com.propertize.platform.auth.security.InternalRequestAuthFilter;
import com.propertize.platform.auth.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final InternalRequestAuthFilter internalRequestAuthFilter;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // InternalRequestAuthFilter runs before Spring Security's own authN checks,
                // injecting a principal derived from the gateway-forwarded headers or service
                // API key.
                .addFilterBefore(internalRequestAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth

                        // ── Truly public authentication endpoints ──────────────────────────────
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/auth/public-key",
                                "/api/v1/auth/.well-known/jwks.json",
                                "/.well-known/jwks.json")
                        .permitAll()

                        // ── Public read-only RBAC catalog (frontend fetches role/permission list) ─
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/rbac/roles",
                                "/api/v1/rbac/permissions",
                                "/api/v1/rbac/config",
                                "/api/v1/auth/rbac/roles",
                                "/api/v1/auth/rbac/permissions",
                                "/api/v1/auth/rbac/config",
                                "/api/v1/auth/roles",
                                "/api/v1/auth/permissions/all")
                        .permitAll()

                        // ── Health: only the liveness probe is public; other actuator paths are not ──
                        .requestMatchers("/api/health", "/actuator/health", "/actuator/health/liveness")
                        .permitAll()

                        // ── All other endpoints (RBAC admin, users, delegations, etc.)
                        // require the request to have come through the API gateway
                        // (validated by InternalRequestAuthFilter) or carry a service API key.
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Allow only known frontend origins — never use wildcard with credentials
        config.setAllowedOrigins(List.of(frontendUrl, "http://localhost:3000"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * Prevent InternalRequestAuthFilter from being auto-registered as a servlet
     * filter by the container. It is only used inside the Spring Security filter
     * chain (via addFilterBefore), so double-registration must be suppressed.
     */
    @Bean
    public FilterRegistrationBean<InternalRequestAuthFilter> internalRequestAuthFilterRegistration(
            InternalRequestAuthFilter filter) {
        FilterRegistrationBean<InternalRequestAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
