package com.propertize.payroll.client;

import com.propertize.payroll.client.dto.EmployeeDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Decorator (GoF Decorator + Circuit Breaker pattern).
 *
 * <p>
 * Wraps {@link EmployecraftFeignClient} with Resilience4j circuit-breaker
 * and retry annotations. Callers depend on this class rather than the raw
 * Feign interface, keeping resilience concerns out of business logic.
 * </p>
 *
 * <h3>Pattern rationale</h3>
 * <ul>
 * <li><b>Circuit Breaker</b>: opens after {@code employecraft} threshold
 * (configured in {@code application.yml}) and returns an empty
 * {@link Optional} so callers can apply caching/defaults.</li>
 * <li><b>Retry</b>: 3 attempts with exponential back-off for transient
 * network hiccups before the circuit counts a failure.</li>
 * <li><b>Decorator</b>: adds cross-cutting concerns without touching the
 * generated Feign proxy or the business service.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ResilientEmployeeClient {

    private static final String CB_NAME = "employecraft";

    private final EmployecraftFeignClient delegate;

    /**
     * Fetches an employee with circuit-breaker and retry protection.
     *
     * @param employeeId    the employee UUID
     * @param authorization bearer token forwarded from the original request
     * @return employee DTO, or {@link Optional#empty()} when the circuit is open
     */
    @CircuitBreaker(name = CB_NAME, fallbackMethod = "employeeFallback")
    @Retry(name = CB_NAME)
    public Optional<EmployeeDto> getEmployee(UUID employeeId, String authorization) {
        ResponseEntity<EmployeeDto> response = delegate.getEmployee(employeeId, authorization);

        return Optional.ofNullable(response.getBody());
    }

    // -------------------------------------------------------------------------
    // Fallback methods — must have the same signature + a Throwable parameter
    // -------------------------------------------------------------------------

    @SuppressWarnings("unused")
    private Optional<EmployeeDto> employeeFallback(UUID employeeId,
            String authorization,
            Throwable cause) {
        log.warn("Circuit breaker OPEN for employecraft — returning empty for employee={}. Cause: {}",
                employeeId, cause.getMessage());
        return Optional.empty();
    }
}
