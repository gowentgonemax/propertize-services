package com.propertize.payroll.client;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuration for Feign Clients
 */
@Configuration
public class FeignClientConfig {

    /**
     * Feign logger level for debugging
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Request options with timeouts
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            5, TimeUnit.SECONDS,  // Connect timeout
            30, TimeUnit.SECONDS, // Read timeout
            true                  // Follow redirects
        );
    }

    /**
     * Retryer configuration for failed requests
     */
    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
            100,   // Initial interval (ms)
            1000,  // Max interval (ms)
            3      // Max attempts
        );
    }
}
