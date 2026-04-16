package com.propertize.payroll.config;

import com.propertize.commons.filter.RequestResponseLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers the canonical {@link RequestResponseLoggingFilter} for the Payroll Service.
 *
 * <p>Supersedes the previously disabled {@link com.propertize.payroll.filter.CorrelationIdFilter}
 * which is now decommissioned in favour of this commons-provided implementation.</p>
 */
@Configuration
public class LoggingConfig {

    private static final String SERVICE_NAME = "payroll-service";

    /**
     * Request/response logging filter registration.
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

