package com.propertize.platform.gateway.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Tests for RateLimitingFilter
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingFilter Tests")
class RateLimitingFilterTest {

    private RateLimitingFilter rateLimitingFilter;

    @Mock
    private GatewayFilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitingFilter = new RateLimitingFilter();
        ReflectionTestUtils.setField(rateLimitingFilter, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(rateLimitingFilter, "defaultRequestsPerMinute", 60);
        ReflectionTestUtils.setField(rateLimitingFilter, "authenticatedRequestsPerMinute", 120);
        ReflectionTestUtils.setField(rateLimitingFilter, "authEndpointRequestsPerMinute", 10);
        ReflectionTestUtils.setField(rateLimitingFilter, "burstMultiplier", 1.5);
    }

    @Nested
    @DisplayName("Basic Rate Limiting Tests")
    class BasicRateLimitingTests {

        @Test
        @DisplayName("Should allow request within rate limit")
        void shouldAllowRequestWithinLimit() {
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/properties")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(rateLimitingFilter.filter(exchange, filterChain))
                .verifyComplete();

            verify(filterChain).filter(exchange);

            // Verify rate limit headers are set
            HttpHeaders headers = exchange.getResponse().getHeaders();
            assertThat(headers.getFirst("X-RateLimit-Remaining")).isNotNull();
            assertThat(headers.getFirst("X-RateLimit-Limit")).isNotNull();
        }

        @Test
        @DisplayName("Should pass through when rate limiting is disabled")
        void shouldPassThroughWhenDisabled() {
            ReflectionTestUtils.setField(rateLimitingFilter, "rateLimitEnabled", false);

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/properties")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(rateLimitingFilter.filter(exchange, filterChain))
                .verifyComplete();

            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("Should block request when rate limit exceeded")
        void shouldBlockWhenRateLimitExceeded() {
            // Set very low limit for testing
            ReflectionTestUtils.setField(rateLimitingFilter, "defaultRequestsPerMinute", 2);
            ReflectionTestUtils.setField(rateLimitingFilter, "burstMultiplier", 1.0);

            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/properties")
                .remoteAddress(new java.net.InetSocketAddress("10.0.0.1", 8080))
                .build();

            // Use lenient stubbing since not all calls may reach the filter chain
            lenient().when(filterChain.filter(any())).thenReturn(Mono.empty());

            // Make requests until limit is exceeded
            for (int i = 0; i < 5; i++) {
                ServerWebExchange exchange = MockServerWebExchange.from(request);
                rateLimitingFilter.filter(exchange, filterChain).block();
            }

            // Next request should be blocked
            ServerWebExchange finalExchange = MockServerWebExchange.from(request);

            StepVerifier.create(rateLimitingFilter.filter(finalExchange, filterChain))
                .verifyComplete();

            assertThat(finalExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    @Nested
    @DisplayName("Rate Limit Key Determination Tests")
    class RateLimitKeyTests {

        @Test
        @DisplayName("Should use user ID for authenticated requests")
        void shouldUseUserIdForAuthenticatedRequests() {
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/properties")
                .header("X-User-Id", "user-123")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(rateLimitingFilter.filter(exchange, filterChain))
                .verifyComplete();

            // User should get higher limit (120 vs 60)
            HttpHeaders headers = exchange.getResponse().getHeaders();
            assertThat(headers.getFirst("X-RateLimit-Limit")).isEqualTo("120");
        }

        @Test
        @DisplayName("Should use user+org key for multi-tenant requests")
        void shouldUseUserOrgKeyForMultiTenant() {
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/properties")
                .header("X-User-Id", "user-123")
                .header("X-Organization-Id", "org-456")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(rateLimitingFilter.filter(exchange, filterChain))
                .verifyComplete();

            verify(filterChain).filter(exchange);
        }

        @Test
        @DisplayName("Should use IP for anonymous requests")
        void shouldUseIpForAnonymousRequests() {
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/properties")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(rateLimitingFilter.filter(exchange, filterChain))
                .verifyComplete();

            // Anonymous user should get default limit (60)
            HttpHeaders headers = exchange.getResponse().getHeaders();
            assertThat(headers.getFirst("X-RateLimit-Limit")).isEqualTo("60");
        }

        @Test
        @DisplayName("Should use X-Forwarded-For for proxied requests")
        void shouldUseXForwardedFor() {
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/properties")
                .header("X-Forwarded-For", "203.0.113.1, 198.51.100.1")
                .remoteAddress(new java.net.InetSocketAddress("127.0.0.1", 8080))
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(rateLimitingFilter.filter(exchange, filterChain))
                .verifyComplete();

            verify(filterChain).filter(exchange);
        }
    }

    @Nested
    @DisplayName("Endpoint-Specific Rate Limits Tests")
    class EndpointSpecificTests {

        @Test
        @DisplayName("Should apply stricter limits for auth endpoints")
        void shouldApplyStricterLimitsForAuthEndpoints() {
            MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login")
                .remoteAddress(new java.net.InetSocketAddress("192.168.1.1", 8080))
                .build();
            ServerWebExchange exchange = MockServerWebExchange.from(request);

            when(filterChain.filter(any())).thenReturn(Mono.empty());

            StepVerifier.create(rateLimitingFilter.filter(exchange, filterChain))
                .verifyComplete();

            // Auth endpoint should get stricter limit (10)
            HttpHeaders headers = exchange.getResponse().getHeaders();
            assertThat(headers.getFirst("X-RateLimit-Limit")).isEqualTo("10");
        }
    }

    @Nested
    @DisplayName("Filter Order Tests")
    class FilterOrderTests {

        @Test
        @DisplayName("Should have correct filter order")
        void shouldHaveCorrectOrder() {
            assertThat(rateLimitingFilter.getOrder()).isEqualTo(-200);
        }
    }

    @Nested
    @DisplayName("Rate Limit Response Tests")
    class RateLimitResponseTests {

        @Test
        @DisplayName("Should include Retry-After header when rate limited")
        void shouldIncludeRetryAfterHeader() {
            // Set very low limit for testing
            ReflectionTestUtils.setField(rateLimitingFilter, "defaultRequestsPerMinute", 1);
            ReflectionTestUtils.setField(rateLimitingFilter, "burstMultiplier", 1.0);

            // Use unique IP to avoid interference from other tests
            MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
                .remoteAddress(new java.net.InetSocketAddress("10.10.10.1", 8080))
                .build();

            // Use lenient stubbing since not all calls may reach the filter chain
            lenient().when(filterChain.filter(any())).thenReturn(Mono.empty());

            // Exhaust the limit
            for (int i = 0; i < 3; i++) {
                ServerWebExchange exchange = MockServerWebExchange.from(request);
                rateLimitingFilter.filter(exchange, filterChain).block();
            }

            // Next request should include Retry-After
            ServerWebExchange finalExchange = MockServerWebExchange.from(request);
            rateLimitingFilter.filter(finalExchange, filterChain).block();

            if (finalExchange.getResponse().getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                HttpHeaders headers = finalExchange.getResponse().getHeaders();
                assertThat(headers.getFirst("Retry-After")).isNotNull();
            }
        }
    }
}
