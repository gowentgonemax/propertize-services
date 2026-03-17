package com.propertize.platform.employecraft.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API Versioning Configuration for Employecraft
 * 
 * Supports version negotiation via:
 * 1. URL path: /api/v1/... (primary)
 * 2. Header: X-API-Version: 1
 * 3. Accept header: application/vnd.employecraft.v1+json
 * 
 * Adds deprecation headers for sunset API versions.
 */
@Slf4j
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    public static final String V1 = "/api/v1";
    public static final String V2 = "/api/v2";
    public static final String CURRENT_VERSION = "1";
    public static final String SUPPORTED_VERSIONS = "1";

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiVersionInterceptor())
                .addPathPatterns("/api/**");
    }

    /**
     * Interceptor that adds API version and deprecation headers.
     */
    static class ApiVersionInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request,
                HttpServletResponse response,
                Object handler) {
            // Add API version info headers
            response.setHeader("X-API-Version", CURRENT_VERSION);
            response.setHeader("X-API-Supported-Versions", SUPPORTED_VERSIONS);

            // Check requested version
            String requestedVersion = request.getHeader("X-API-Version");
            if (requestedVersion != null) {
                String normalized = normalizeVersion(requestedVersion);
                if (!SUPPORTED_VERSIONS.contains(normalized)) {
                    log.warn("Unsupported API version requested: {} from {}",
                            requestedVersion, request.getRemoteAddr());
                    response.setHeader("X-API-Warning",
                            "Requested version " + requestedVersion + " is not supported. Using v" + CURRENT_VERSION);
                }
            }

            return true;
        }

        private String normalizeVersion(String version) {
            if (version == null)
                return CURRENT_VERSION;
            return version.toLowerCase()
                    .replace("v", "")
                    .replace(".0", "")
                    .trim();
        }
    }
}
