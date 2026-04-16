package com.propertize.platform.employecraft.config;

import com.propertize.commons.filter.RequestResponseLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers the canonical {@link RequestResponseLoggingFilter} for the Employee Service.
 *
 * <p>Runs after the existing {@link com.propertize.platform.employecraft.filter.CorrelationIdFilter}
 * (order = HIGHEST_PRECEDENCE) so MDC context is already set when the request is logged.</p>
 */
@Configuration
public class LoggingConfig {

    private static final String SERVICE_NAME = "employee-service";

    /**
     * Request/response logging filter — logs every HTTP exchange with method, URI,
     * status, duration, correlation-ID and user context.
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

