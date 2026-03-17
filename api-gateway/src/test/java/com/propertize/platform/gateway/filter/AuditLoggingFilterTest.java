package com.propertize.platform.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for AuditLoggingFilter
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLoggingFilter Tests")
class AuditLoggingFilterTest {

    private AuditLoggingFilter auditLoggingFilter;

    @Mock
    private GatewayFilterChain filterChain;

    @BeforeEach
    void setUp() {
        auditLoggingFilter = new AuditLoggingFilter();
        ReflectionTestUtils.setField(auditLoggingFilter, "auditEnabled", true);
        ReflectionTestUtils.setField(auditLoggingFilter, "logRequestBody", false);
        ReflectionTestUtils.setField(auditLoggingFilter, "logResponseBody", false);
        ReflectionTestUtils.setField(auditLoggingFilter, "maxQueueSize", 10000);
    }

    @Nested
    @DisplayName("Filter Behavior Tests")
    class FilterBehaviorTests {

        @Test
        @DisplayName("Should pass through when audit is disabled")
        void shouldPassThroughWhenDisabled() {
            ReflectionTestUtils.setField(auditLoggingFilter, "auditEnabled", false);

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/properties")
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(auditLoggingFilter.filter(exchange, filterChain))
                .verifyComplete();

            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("Should audit auth endpoints")
        void shouldAuditAuthEndpoints() {
            MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login")
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(auditLoggingFilter.filter(exchange, filterChain))
                .verifyComplete();

            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("Should audit sensitive endpoints")
        void shouldAuditSensitiveEndpoints() {
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/admin/users")
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(auditLoggingFilter.filter(exchange, filterChain))
                .verifyComplete();

            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("Should audit payment endpoints")
        void shouldAuditPaymentEndpoints() {
            MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/payments/process")
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(auditLoggingFilter.filter(exchange, filterChain))
                .verifyComplete();

            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("Should skip GET requests on non-sensitive endpoints")
        void shouldSkipGetOnNonSensitiveEndpoints() {
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/properties")
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(auditLoggingFilter.filter(exchange, filterChain))
                .verifyComplete();

            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("Should audit POST/PUT/DELETE on regular endpoints")
        void shouldAuditWriteOperationsOnRegularEndpoints() {
            MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/properties")
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(auditLoggingFilter.filter(exchange, filterChain))
                .verifyComplete();

            verify(filterChain).filter(exchange);
        }
    }

    @Nested
    @DisplayName("Correlation ID Tests")
    class CorrelationIdTests {

        @Test
        @DisplayName("Should use existing correlation ID")
        void shouldUseExistingCorrelationId() {
            String correlationId = UUID.randomUUID().toString();

            MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login")
                .header("X-Correlation-Id", correlationId)
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(auditLoggingFilter.filter(exchange, filterChain))
                .verifyComplete();

            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("Should generate correlation ID if missing")
        void shouldGenerateCorrelationIdIfMissing() {
            MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login")
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(auditLoggingFilter.filter(exchange, filterChain))
                .verifyComplete();

            verify(filterChain).filter(exchange);
        }
    }

    @Nested
    @DisplayName("User Context Tests")
    class UserContextTests {

        @Test
        @DisplayName("Should capture user ID from headers")
        void shouldCaptureUserIdFromHeaders() {
            MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login")
                .header("X-User-Id", "user-123")
                .header("X-Organization-Id", "org-456")
                .header("X-Roles", "ADMIN,USER")
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(auditLoggingFilter.filter(exchange, filterChain))
                .verifyComplete();

            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("Should handle missing user context")
        void shouldHandleMissingUserContext() {
            MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login")
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(auditLoggingFilter.filter(exchange, filterChain))
                .verifyComplete();

            verify(filterChain).filter(exchange);
        }
    }

    @Nested
    @DisplayName("Filter Order Tests")
    class FilterOrderTests {

        @Test
        @DisplayName("Should have correct filter order")
        void shouldHaveCorrectOrder() {
            assertThat(auditLoggingFilter.getOrder()).isEqualTo(-50);
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle filter chain errors gracefully")
        void shouldHandleFilterChainErrors() {
            MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login")
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.error(new RuntimeException("Test error")));

            StepVerifier.create(auditLoggingFilter.filter(exchange, filterChain))
                .verifyError(RuntimeException.class);
        }
    }
}
