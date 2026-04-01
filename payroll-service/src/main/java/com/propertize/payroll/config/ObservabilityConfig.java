package com.propertize.payroll.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Observability configuration for the payroll-service.
 *
 * <h3>What this provides</h3>
 * <ul>
 * <li><b>MDC Correlation ID filter</b>: injects {@code X-Correlation-ID}
 * (or generates a UUID) into SLF4J's MDC on every request so all log
 * lines carry the same trace token. Header is echoed back in the
 * response for client-side tracing.</li>
 * <li><b>JVM thread metrics</b>: exposes virtual-thread counts under
 * {@code jvm.threads.*} for Prometheus scraping.</li>
 * <li><b>Global metric tags</b>: adds {@code service} and {@code env}
 * labels to every Micrometer metric.</li>
 * </ul>
 *
 * <h3>Prometheus scrape target</h3>
 * {@code GET /actuator/prometheus} — already exposed via
 * {@code management.endpoints.web.exposure.include}.
 */
@Configuration
@Slf4j
public class ObservabilityConfig {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_CORRELATION_KEY = "correlationId";

    // -----------------------------------------------------------------------
    // MDC correlation-ID filter
    // -----------------------------------------------------------------------

    /**
     * Servlet filter that ensures every request has a correlation ID in MDC.
     * This makes structured log output (JSON) automatically carry the ID
     * without any manual plumbing in controllers or services.
     */
    @Bean
    public OncePerRequestFilter correlationIdFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                    HttpServletResponse response,
                    FilterChain filterChain)
                    throws ServletException, IOException {

                String correlationId = request.getHeader(CORRELATION_ID_HEADER);
                if (correlationId == null || correlationId.isBlank()) {
                    correlationId = UUID.randomUUID().toString();
                }

                // Populate MDC so Logback/Log4j2 JSON layouts pick it up automatically
                MDC.put(MDC_CORRELATION_KEY, correlationId);
                response.setHeader(CORRELATION_ID_HEADER, correlationId);

                try {
                    filterChain.doFilter(request, response);
                } finally {
                    // Always clear — virtual threads reuse carriers but MDC leaks if not cleared
                    MDC.remove(MDC_CORRELATION_KEY);
                }
            }
        };
    }

    // -----------------------------------------------------------------------
    // Micrometer customizations
    // -----------------------------------------------------------------------

    /**
     * Adds global tags to every metric — visible in Prometheus as
     * {@code service="payroll-service"} labels, enabling per-service dashboards.
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> commonTags() {
        return registry -> registry.config()
                .commonTags("service", "payroll-service");
    }

    /**
     * Publishes JVM thread metrics including virtual thread counts.
     * This surfaces as {@code jvm_threads_*} in the Prometheus scrape.
     */
    @Bean
    public JvmThreadMetrics jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }
}
