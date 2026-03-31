package com.propertize.commons.exception;

import com.propertize.commons.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link PropertizeGlobalExceptionHandler}.
 *
 * Run with: mvn test -pl propertize-commons
 */
class PropertizeGlobalExceptionHandlerTest {

    private PropertizeGlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new PropertizeGlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "test-cid-001");
    }

    @Nested
    @DisplayName("ResourceNotFoundException")
    class NotFoundTests {

        @Test
        void returns404WithCorrectCode() {
            var ex = new ResourceNotFoundException("Employee", 42L);
            ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(404);
            assertThat(response.getBody().code()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
            assertThat(response.getBody().message()).contains("Employee").contains("42");
            assertThat(response.getBody().correlationId()).isEqualTo("test-cid-001");
        }
    }

    @Nested
    @DisplayName("InvalidStateTransitionException")
    class StateTransitionTests {

        @Test
        void returns409OnInvalidTransition() {
            var ex = new InvalidStateTransitionException("PayrollRun", "PENDING", "APPROVED");
            ResponseEntity<ErrorResponse> response = handler.handleInvalidTransition(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody().code()).isEqualTo(ErrorCode.INVALID_STATE_TRANSITION);
            assertThat(response.getBody().message()).contains("PENDING").contains("APPROVED");
        }
    }

    @Nested
    @DisplayName("Validation errors")
    class ValidationTests {

        @Test
        void returns400WithFieldErrors() throws Exception {
            // Simulate bean validation failure on field "email"
            var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
            bindingResult.addError(new FieldError("request", "email", "must not be blank"));

            var ex = new MethodArgumentNotValidException(null, bindingResult);
            ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().code()).isEqualTo(ErrorCode.VALIDATION_FAILED);
            assertThat(response.getBody().fieldErrors()).containsKey("email");
            assertThat(response.getBody().fieldErrors().get("email")).isEqualTo("must not be blank");
        }
    }

    @Nested
    @DisplayName("Generic exception")
    class GenericTests {

        @Test
        void returns500ForUnexpectedException() {
            ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new RuntimeException("boom"), request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().code()).isEqualTo(ErrorCode.INTERNAL_ERROR);
            assertThat(response.getBody().correlationId()).isEqualTo("test-cid-001");
        }

        @Test
        void generatesCorrelationIdWhenHeaderAbsent() {
            var req = new MockHttpServletRequest(); // no header
            ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new RuntimeException("no header"), req);

            assertThat(response.getBody().correlationId()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("UpstreamServiceException")
    class UpstreamTests {

        @Test
        void returns502ForUpstreamFailure() {
            var ex = new UpstreamServiceException("employee-service", "connection refused");
            ResponseEntity<ErrorResponse> response = handler.handleUpstream(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
            assertThat(response.getBody().code()).isEqualTo(ErrorCode.UPSTREAM_SERVICE_ERROR);
            assertThat(response.getBody().message()).contains("employee-service");
        }
    }
}
