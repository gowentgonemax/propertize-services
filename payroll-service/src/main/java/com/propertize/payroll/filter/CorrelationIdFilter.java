package com.propertize.payroll.filter;

import com.propertize.payroll.config.CorrelationIdUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filter to add correlation ID to all HTTP requests for tracking
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Try to get correlation ID from request header, or generate new one
            String correlationId = httpRequest.getHeader(CorrelationIdUtil.getHeaderName());

            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = CorrelationIdUtil.generateCorrelationId();
                log.debug("Generated new correlation ID: {}", correlationId);
            } else {
                log.debug("Using existing correlation ID: {}", correlationId);
            }

            // Set correlation ID in MDC for logging
            CorrelationIdUtil.setCorrelationId(correlationId);

            // Add correlation ID to response header
            httpResponse.setHeader(CorrelationIdUtil.getHeaderName(), correlationId);

            log.info("Request received: {} {} - Correlation ID: {}",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    correlationId);

            // Continue with the request
            chain.doFilter(request, response);

            log.info("Request completed: {} {} - Correlation ID: {}",
                    httpRequest.getMethod(),
                    httpRequest.getRequestURI(),
                    correlationId);

        } finally {
            // Always clear MDC after request is processed to prevent memory leaks
            CorrelationIdUtil.clear();
        }
    }
}

