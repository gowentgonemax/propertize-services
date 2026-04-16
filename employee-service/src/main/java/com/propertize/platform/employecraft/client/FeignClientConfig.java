package com.propertize.platform.employecraft.client;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign Client Configuration
 */
@Configuration
public class FeignClientConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
                5000, TimeUnit.MILLISECONDS, // Connect timeout
                10000, TimeUnit.MILLISECONDS, // Read timeout
                true // Follow redirects
        );
    }

    /**
     * Retry up to 3 times with 500 ms initial backoff (doubles each attempt,
     * max 2 s) for transient network failures.
     */
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(
                500, // initial interval ms
                2_000, // max interval ms
                3 // max attempts
        );
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }
}
