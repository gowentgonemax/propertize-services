package com.propertize.platform.auth.security;

import com.propertize.platform.auth.config.ServiceAuthenticationConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * InternalRequestAuthFilter — establishes a Spring Security principal for
 * requests to the auth-service that arrived from the API gateway.
 *
 * The API gateway validates the user JWT (RS256), then forwards the verified
 * context as trusted headers (X-Gateway-Source, X-Roles, X-User-Id).
 * This filter trusts those headers to populate the SecurityContext so that
 * Spring Security's .anyRequest().authenticated() rule is satisfied.
 *
 * Service-to-service calls (internal microservices) can alternatively use
 * the X-Service-Api-Key header.
 *
 * Public paths (login, refresh, etc.) bypass all of this — they are already
 * marked permitAll() in SecurityConfig.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternalRequestAuthFilter extends OncePerRequestFilter {

    private static final String GATEWAY_SOURCE_HEADER = "X-Gateway-Source";
    private static final String ROLES_HEADER = "X-Roles";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String SERVICE_API_KEY_HEADER = "X-Service-Api-Key";
    private static final String GATEWAY_SOURCE_VALUE = "api-gateway";

    private final ServiceAuthenticationConfig serviceAuthConfig;

    @Value("${service.authentication.apiKey:}")
    private String serviceApiKey;

    /**
     * Shared secret for HMAC gateway header verification. Empty = verification
     * skipped.
     */
    @Value("${security.gateway.hmac-secret:}")
    private String hmacSecret;

    // Paths that are completely open — SecurityConfig already permits them, but
    // listed here so the filter knows not to attempt to set a principal.
    private static final Set<String> SKIP_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/public-key",
            "/.well-known/jwks.json",
            "/api/health",
            "/actuator/health",
            "/actuator/health/liveness",
            "/swagger-ui.html");

    private static final Set<String> SKIP_PATH_PREFIXES = Set.of(
            "/api/v1/rbac/",
            "/api/v1/auth/rbac/",
            "/api/v1/auth/roles",
            "/api/v1/auth/permissions/all",
            "/actuator/health",
            "/swagger-ui/",
            "/v3/api-docs",
            "/api-docs",
            "/webjars/");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Already-open paths — skip; SecurityConfig permits them.
        if (isSkippedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // If SecurityContext already has authentication (should not normally happen in
        // stateless mode, but guard against double-processing).
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // ── Option 1: Service-to-service API key ──────────────────────────────────
        String serviceKey = request.getHeader(SERVICE_API_KEY_HEADER);
        if (serviceKey != null && !serviceKey.isBlank() && isValidServiceApiKey(serviceKey)) {
            String serviceName = request.getHeader("X-Service-Name");
            log.debug("Service-to-service request authenticated: service={}, path={}", serviceName, path);
            setServiceAuthentication(serviceName != null ? serviceName : "internal-service");
            filterChain.doFilter(request, response);
            return;
        }

        // ── Option 2: Gateway-forwarded user context ───────────────────────────────
        String gatewaySource = request.getHeader(GATEWAY_SOURCE_HEADER);
        String rolesHeader = request.getHeader(ROLES_HEADER);

        if (GATEWAY_SOURCE_VALUE.equals(gatewaySource) && rolesHeader != null && !rolesHeader.isBlank()) {
            String userId = request.getHeader(USER_ID_HEADER);

            // Verify HMAC signature when configured
            if (!hmacSecret.isBlank()) {
                String signature = request.getHeader("X-Gateway-Signature");
                if (!isValidGatewaySignature(signature, userId, rolesHeader)) {
                    log.warn("Invalid or missing gateway HMAC signature for path={}", path);
                    sendUnauthorized(response, "Invalid gateway signature");
                    return;
                }
            }

            List<SimpleGrantedAuthority> authorities = parseRoles(rolesHeader);
            log.debug("Gateway-forwarded request: userId={}, roles={}, path={}", userId, rolesHeader, path);
            setUserAuthentication(userId != null ? userId : "unknown", authorities);
            filterChain.doFilter(request, response);
            return;
        }

        // ── No valid authentication source found ───────────────────────────────────
        log.warn("Unauthenticated access attempt blocked: path={}, ip={}",
                path, request.getRemoteAddr());
        sendUnauthorized(response, "Authentication required. "
                + "Requests must arrive via API gateway or carry a service API key.");
    }

    private boolean isSkippedPath(String path) {
        if (SKIP_PATHS.contains(path))
            return true;
        return SKIP_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private boolean isValidServiceApiKey(String key) {
        // Check this service's own API key
        if (serviceApiKey != null && !serviceApiKey.isBlank() && serviceApiKey.equals(key)) {
            return true;
        }
        // Check trusted services map (service-to-service calls from propertize,
        // employee-service, etc.)
        Map<String, String> trustedServices = serviceAuthConfig.getTrustedServices();
        if (trustedServices != null) {
            return trustedServices.values().stream().anyMatch(key::equals);
        }
        return false;
    }

    private List<SimpleGrantedAuthority> parseRoles(String rolesHeader) {
        if (rolesHeader == null || rolesHeader.isBlank())
            return Collections.emptyList();
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(r -> !r.isEmpty())
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                .collect(Collectors.toList());
    }

    private void setUserAuthentication(String principal, List<SimpleGrantedAuthority> authorities) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null,
                authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setServiceAuthentication(String serviceName) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                serviceName, null,
                List.of(new SimpleGrantedAuthority("ROLE_SERVICE")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"success\":false,\"error\":\"" + message + "\",\"status\":401}");
    }

    private boolean isValidGatewaySignature(String signature, String userId, String roles) {
        if (signature == null || userId == null) {
            return false;
        }
        long epochMinutes = Instant.now().getEpochSecond() / 60;
        return signature.equals(computeHmac(userId + ":" + roles + ":" + epochMinutes))
                || signature.equals(computeHmac(userId + ":" + roles + ":" + (epochMinutes - 1)));
    }

    private String computeHmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            log.error("HMAC computation failed", e);
            return "";
        }
    }
}
