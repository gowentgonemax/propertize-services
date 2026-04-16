package com.propertize.platform.auth.config;

import com.propertize.commons.filter.RequestResponseLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers the canonical {@link RequestResponseLoggingFilter} for the Auth Service.
 *
 * <p>Every inbound HTTP request and outbound response is captured with:
 * method, URI, status code, duration, correlation-ID, user-ID, and client IP.
 * Sensitive headers (Authorization, Cookie) are always masked.
 * Response bodies are logged at DEBUG level only for error responses.</p>
 */
@Configuration
public class LoggingConfig {

    private static final String SERVICE_NAME = "auth-service";

    /**
     * Registers the request/response logging filter at order HIGHEST_PRECEDENCE + 10
     * so the correlation-ID filter (order HIGHEST_PRECEDENCE) runs first and the MDC
     * context is already populated when this filter fires.
     *
     * @return configured filter registration bean
     */
    @Bean
    public FilterRegistrationBean<RequestResponseLoggingFilter> requestResponseLoggingFilter() {
        FilterRegistrationBean<RequestResponseLoggingFilter> bean =
                new FilterRegistrationBean<>(new RequestResponseLoggingFilter(SERVICE_NAME));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        bean.addUrlPatterns("/*");
        return bean;
    }
}

