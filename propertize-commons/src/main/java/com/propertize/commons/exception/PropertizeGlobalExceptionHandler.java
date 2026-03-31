package com.propertize.commons.exception;

import com.propertize.commons.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Drop-in global exception handler for all Propertize Spring Boot services.
 *
 * <h3>Usage — add to your service's component scan or auto-configure:</h3>
 * 
 * <pre>{@code
 * // Option A — component scan picks it up automatically if
 * // com.propertize.commons is in your base-packages.
 *
 * // Option B — declare a bean in your own @Configuration:
 * @Bean
 * public PropertizeGlobalExceptionHandler globalExceptionHandler() {
 *     return new PropertizeGlobalExceptionHandler();
 * }
 * }</pre>
 *
 * <h3>Migration from per-service GlobalExceptionHandler:</h3>
 * <ol>
 * <li>Add {@code propertize-commons} dependency to your pom.xml</li>
 * <li>Delete (or keep but mark @Deprecated) your local
 * GlobalExceptionHandler</li>
 * <li>Ensure your exceptions extend {@link BaseException}</li>
 * <li>Verify tests pass — response shape is identical</li>
 * </ol>
 */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class PropertizeGlobalExceptionHandler {

    // ── BaseException hierarchy (all custom domain exceptions) ───────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        String cid = correlationId(req);
        log.warn("[{}] Resource not found: {}", cid, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getErrorCode(), ex.getMessage(), cid));
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransition(InvalidStateTransitionException ex,
            HttpServletRequest req) {
        String cid = correlationId(req);
        log.warn("[{}] Invalid state transition: {}", cid, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getErrorCode(), ex.getMessage(), cid));
    }

    @ExceptionHandler(UpstreamServiceException.class)
    public ResponseEntity<ErrorResponse> handleUpstream(UpstreamServiceException ex, HttpServletRequest req) {
        String cid = correlationId(req);
        log.error("[{}] Upstream service error: {}", cid, ex.getMessage(), ex.getCause());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of(502, "Bad Gateway", ex.getErrorCode(), ex.getMessage(), cid));
    }

    /** Catch-all for any other BaseException subclass not handled above. */
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBase(BaseException ex, HttpServletRequest req) {
        String cid = correlationId(req);
        HttpStatus status = httpStatusFor(ex.getErrorCode());
        log.warn("[{}] Business exception [{}]: {}", cid, ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), status.getReasonPhrase(), ex.getErrorCode(), ex.getMessage(),
                        cid));
    }

    // ── Spring / JVM built-ins ────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
            HttpServletRequest req) {
        String cid = correlationId(req);
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                        (a, b) -> a));
        log.warn("[{}] Validation failed: {}", cid, fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.validationFailed(fieldErrors, cid));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        String cid = correlationId(req);
        log.warn("[{}] Bad argument: {}", cid, ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of(400, "Bad Request", ErrorCode.BAD_REQUEST, ex.getMessage(), cid));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, HttpServletRequest req) {
        String cid = correlationId(req);
        log.warn("[{}] Illegal state: {}", cid, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ErrorCode.CONFLICT, ex.getMessage(), cid));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
            HttpServletRequest req) {
        String cid = correlationId(req);
        log.error("[{}] Data integrity violation", cid);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ErrorCode.CONFLICT,
                        "A resource with the same identifier already exists.", cid));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
        String cid = correlationId(req);
        log.error("[{}] Unexpected error", cid, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error", ErrorCode.INTERNAL_ERROR,
                        "An unexpected error occurred. Reference: " + cid, cid));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String correlationId(HttpServletRequest req) {
        String fromHeader = req.getHeader("X-Correlation-ID");
        return (fromHeader != null && !fromHeader.isBlank()) ? fromHeader : UUID.randomUUID().toString();
    }

    /**
     * Map ErrorCode to HTTP status.
     * Pattern matching switch — Java 21 feature.
     */
    private HttpStatus httpStatusFor(ErrorCode code) {
        return switch (code) {
            case RESOURCE_NOT_FOUND, EMPLOYEE_NOT_FOUND, PAYROLL_RUN_NOT_FOUND,
                    USER_NOT_FOUND ->
                HttpStatus.NOT_FOUND;
            case ACCESS_DENIED, USER_ROLE_INSUFFICIENT -> HttpStatus.FORBIDDEN;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case UPSTREAM_SERVICE_ERROR -> HttpStatus.BAD_GATEWAY;
            case UPSTREAM_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case CONFLICT, EMPLOYEE_ALREADY_EXISTS, USER_EMAIL_TAKEN,
                    PAYROLL_RUN_ALREADY_PROCESSED ->
                HttpStatus.CONFLICT;
            case INVALID_STATE_TRANSITION, PAYROLL_TIMESHEET_NOT_SUBMITTED -> HttpStatus.CONFLICT;
            case VALIDATION_FAILED, BAD_REQUEST, EMPLOYEE_INVALID_DEPARTMENT -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
