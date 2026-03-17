package com.propertize.platform.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Circuit Breaker configuration for API Gateway
 *
 * <p>Provides resilience patterns for downstream service calls:</p>
 * <ul>
 *   <li>Circuit Breaker: Prevents cascading failures</li>
 *   <li>Timeout: Limits slow calls</li>
 *   <li>Fallback: Provides alternative responses</li>
 * </ul>
 *
 * @author Platform Team
 * @since 1.0.0
 */
@Configuration
public class CircuitBreakerConfig {

    /**
     * Configures circuit breaker for Auth Service calls
     *
     * <p>Configuration:</p>
     * <ul>
     *   <li>Failure Rate Threshold: 50%</li>
     *   <li>Slow Call Threshold: 80%</li>
     *   <li>Wait Duration in Open State: 30s</li>
     *   <li>Permitted Calls in Half-Open: 3</li>
     *   <li>Sliding Window Size: 10 calls</li>
     *   <li>Minimum Calls: 5</li>
     *   <li>Timeout: 2 seconds</li>
     * </ul>
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> authServiceCircuitBreakerCustomizer() {
        return factory -> factory.configure(builder -> builder
                .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        // Circuit opens if 50% of calls fail
                        .failureRateThreshold(50)

                        // Circuit opens if 80% of calls are slow (>1s)
                        .slowCallRateThreshold(80)
                        .slowCallDurationThreshold(Duration.ofSeconds(1))

                        // Wait 30s before transitioning from OPEN to HALF_OPEN
                        .waitDurationInOpenState(Duration.ofSeconds(30))

                        // Allow 3 calls in HALF_OPEN state to test recovery
                        .permittedNumberOfCallsInHalfOpenState(3)

                        // Use count-based sliding window (last 10 calls)
                        .slidingWindowType(SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(10)

                        // Need at least 5 calls before calculating failure rate
                        .minimumNumberOfCalls(5)

                        // Automatically transition from OPEN to HALF_OPEN
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)

                        .build())

                // Timeout configuration
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(2))
                        .cancelRunningFuture(true)
                        .build())

                .build(), "auth-service");
    }

    /**
     * Configures default circuit breaker for all other services
     *
     * <p>More lenient than auth-service configuration:</p>
     * <ul>
     *   <li>Failure Rate Threshold: 60%</li>
     *   <li>Wait Duration: 20s</li>
     *   <li>Timeout: 5 seconds</li>
     * </ul>
     */
    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCircuitBreakerCustomizer() {
        return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
                .circuitBreakerConfig(io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.custom()
                        .failureRateThreshold(60)
                        .slowCallRateThreshold(85)
                        .slowCallDurationThreshold(Duration.ofSeconds(3))
                        .waitDurationInOpenState(Duration.ofSeconds(20))
                        .permittedNumberOfCallsInHalfOpenState(5)
                        .slidingWindowType(SlidingWindowType.COUNT_BASED)
                        .slidingWindowSize(20)
                        .minimumNumberOfCalls(10)
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)
                        .build())
                .timeLimiterConfig(TimeLimiterConfig.custom()
                        .timeoutDuration(Duration.ofSeconds(5))
                        .cancelRunningFuture(true)
                        .build())
                .build());
    }

    /**
     * Provides access to circuit breaker registry for monitoring
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }
}
