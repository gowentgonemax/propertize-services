package com.propertize.commons.filter;

import com.propertize.commons.constants.GatewayHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

/**
 * Reusable HTTP request/response logging filter for all Propertize servlet-based services.
 *
 * <p>Captures every inbound request and outbound response with:</p>
 * <ul>
 *   <li>HTTP method, URI, query string, client IP</li>
 *   <li>Correlation-ID propagated from the gateway (or generated locally)</li>
 *   <li>Authenticated user/org IDs extracted from gateway headers</li>
 *   <li>Response status code and wall-clock duration</li>
 *   <li>Response body at DEBUG level for error responses (status ≥ 400), truncated to 2 KB</li>
 *   <li>Request headers at DEBUG level — sensitive headers are masked</li>
 * </ul>
 *
 * <p>Sensitive headers always masked (value replaced with {@code [REDACTED]}):
 * {@code Authorization}, {@code Cookie}, {@code Set-Cookie}, {@code X-Api-Key}.</p>
 *
 * <p>Paths excluded from logging: {@code /actuator/health*}, {@code /actuator/info},
 * {@code /favicon.ico}.</p>
 *
 * <h3>Registration in a service</h3>
 * <pre>{@code
 * @Configuration
 * public class LoggingConfig {
 *     @Bean
 *     public FilterRegistrationBean<RequestResponseLoggingFilter> requestLoggingFilter() {
 *         FilterRegistrationBean<RequestResponseLoggingFilter> bean =
 *                 new FilterRegistrationBean<>(new RequestResponseLoggingFilter("my-service"));
 *         bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
 *         bean.addUrlPatterns("/*");
 *         return bean;
 *     }
 * }
 * }</pre>
 *
 * @see GatewayHeaders
 */
@Slf4j
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    // ── MDC keys (aligned across all services) ───────────────────────────────
    public static final String MDC_CORRELATION_ID  = "correlationId";
    public static final String MDC_REQUEST_ID      = "requestId";
    public static final String MDC_USER_ID         = "userId";
    public static final String MDC_ORG_ID          = "orgId";
    public static final String MDC_REQUEST_METHOD  = "requestMethod";
    public static final String MDC_REQUEST_URI     = "requestUri";
    public static final String MDC_CLIENT_IP       = "clientIp";
    public static final String MDC_SERVICE_NAME    = "serviceName";

    private static final int    MAX_BODY_LOG_BYTES = 2048;
    private static final long   SLOW_REQUEST_MS    = 3_000L;

    private static final Set<String> MASKED_HEADERS = Set.of(
            "authorization", "cookie", "set-cookie", "x-api-key"
    );

    private static final Set<String> EXCLUDED_PATH_PREFIXES = Set.of(
            "/actuator/health", "/actuator/info", "/favicon.ico"
    );

    private final String serviceName;

    /**
     * @param serviceName short label used in log lines (e.g. {@code "auth-service"})
     */
    public RequestResponseLoggingFilter(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        ContentCachingRequestWrapper  wrappedReq  = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResp = new ContentCachingResponseWrapper(response);

        String correlationId = resolveCorrelationId(request);
        String requestId     = UUID.randomUUID().toString();

        populateMdc(request, correlationId, requestId);
        wrappedResp.setHeader(GatewayHeaders.X_CORRELATION_ID, correlationId);

        long start = System.currentTimeMillis();
        try {
            logIncomingRequest(wrappedReq, correlationId);
            chain.doFilter(wrappedReq, wrappedResp);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            logOutgoingResponse(wrappedReq, wrappedResp, durationMs, correlationId);
            wrappedResp.copyBodyToResponse();
            clearMdc();
        }
    }

    // ── Incoming request ─────────────────────────────────────────────────────

    /**
     * Logs one INFO line per request; headers logged only at DEBUG.
     */
    private void logIncomingRequest(ContentCachingRequestWrapper req, String correlationId) {
        StringBuilder sb = new StringBuilder(128);
        sb.append("▶ REQUEST")
          .append(" | service=").append(serviceName)
          .append(" | ").append(req.getMethod())
          .append(" ").append(req.getRequestURI());

        String qs = req.getQueryString();
        if (qs != null && !qs.isBlank()) {
            sb.append("?").append(qs);
        }

        sb.append(" | ip=").append(extractClientIp(req))
          .append(" | correlationId=").append(correlationId);

        String userId = req.getHeader(GatewayHeaders.X_USER_ID);
        if (userId != null) {
            sb.append(" | userId=").append(userId);
        }

        String orgId = req.getHeader(GatewayHeaders.X_ORGANIZATION_ID);
        if (orgId != null) {
            sb.append(" | orgId=").append(orgId);
        }

        log.info(sb.toString());

        if (log.isDebugEnabled()) {
            logRequestHeaders(req);
        }
    }

    private void logRequestHeaders(HttpServletRequest req) {
        StringBuilder headers = new StringBuilder("  ↳ Headers: ");
        Collections.list(req.getHeaderNames()).forEach(name -> {
            String value = MASKED_HEADERS.contains(name.toLowerCase())
                    ? "[REDACTED]"
                    : req.getHeader(name);
            headers.append(name).append("=").append(value).append("; ");
        });
        log.debug(headers.toString());
    }

    // ── Outgoing response ────────────────────────────────────────────────────

    /**
     * Logs one INFO/WARN/ERROR line per response with status + duration.
     * Logs response body at DEBUG for error responses.
     */
    private void logOutgoingResponse(ContentCachingRequestWrapper req,
                                     ContentCachingResponseWrapper resp,
                                     long durationMs,
                                     String correlationId) {
        int    status = resp.getStatus();
        String method = req.getMethod();
        String uri    = req.getRequestURI();

        StringBuilder sb = new StringBuilder(128);
        sb.append("◀ RESPONSE")
          .append(" | service=").append(serviceName)
          .append(" | ").append(method)
          .append(" ").append(uri)
          .append(" | status=").append(status)
          .append(" | duration=").append(durationMs).append("ms")
          .append(" | correlationId=").append(correlationId);

        if (status >= 500) {
            log.error(sb.toString());
            logResponseBody(resp, status);
        } else if (status >= 400) {
            log.warn(sb.toString());
            logResponseBody(resp, status);
        } else {
            log.info(sb.toString());
        }

        if (durationMs > SLOW_REQUEST_MS) {
            log.warn("⚠ SLOW REQUEST | service={} | {} {} took {}ms [correlationId={}]",
                    serviceName, method, uri, durationMs, correlationId);
        }
    }

    private void logResponseBody(ContentCachingResponseWrapper resp, int status) {
        if (!log.isDebugEnabled()) {
            return;
        }
        byte[] body = resp.getContentAsByteArray();
        if (body.length == 0) {
            return;
        }
        try {
            String charEnc = resp.getCharacterEncoding();
            Charset charset = charEnc != null
                    ? Charset.forName(charEnc)
                    : StandardCharsets.UTF_8;
            int limit = Math.min(body.length, MAX_BODY_LOG_BYTES);
            String snippet = new String(body, 0, limit, charset);
            if (body.length > MAX_BODY_LOG_BYTES) {
                snippet += "... [truncated]";
            }
            log.debug("  ↳ Response body (status={}): {}", status, snippet);
        } catch (Exception e) {
            log.debug("  ↳ Unable to decode response body: {}", e.getMessage());
        }
    }

    // ── MDC management ────────────────────────────────────────────────────────

    private void populateMdc(HttpServletRequest req, String correlationId, String requestId) {
        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_REQUEST_ID,     requestId);
        MDC.put(MDC_REQUEST_METHOD, req.getMethod());
        MDC.put(MDC_REQUEST_URI,    req.getRequestURI());
        MDC.put(MDC_CLIENT_IP,      extractClientIp(req));
        MDC.put(MDC_SERVICE_NAME,   serviceName);

        String userId = req.getHeader(GatewayHeaders.X_USER_ID);
        if (userId != null && !userId.isBlank()) {
            MDC.put(MDC_USER_ID, userId);
        }

        String orgId = req.getHeader(GatewayHeaders.X_ORGANIZATION_ID);
        if (orgId != null && !orgId.isBlank()) {
            MDC.put(MDC_ORG_ID, orgId);
        }
    }

    private void clearMdc() {
        MDC.remove(MDC_CORRELATION_ID);
        MDC.remove(MDC_REQUEST_ID);
        MDC.remove(MDC_REQUEST_METHOD);
        MDC.remove(MDC_REQUEST_URI);
        MDC.remove(MDC_CLIENT_IP);
        MDC.remove(MDC_SERVICE_NAME);
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_ORG_ID);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String resolveCorrelationId(HttpServletRequest req) {
        String id = req.getHeader(GatewayHeaders.X_CORRELATION_ID);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        return id;
    }

    private String extractClientIp(HttpServletRequest req) {
        return Stream.of(
                req.getHeader("X-Forwarded-For"),
                req.getHeader("X-Real-IP"),
                req.getRemoteAddr()
        ).filter(ip -> ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip))
         .findFirst()
         .map(ip -> ip.contains(",") ? ip.split(",")[0].trim() : ip)
         .orElse("unknown");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String uri = req.getRequestURI();
        return EXCLUDED_PATH_PREFIXES.stream().anyMatch(uri::startsWith);
    }
}


