package com.propertize.commons.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.propertize.commons.exception.ErrorCode;

import java.time.Instant;
import java.util.Map;

/**
 * Canonical error response body returned by all Propertize services.
 *
 * Example JSON:
 * <pre>{@code
 * {
 * "status": 404,
 * "error": "Not Found",
 * "code": "EMPLOYEE_NOT_FOUND",
 * "message": "Employee not found with id: 42",
 * "timestamp": "2026-03-29T10:15:30Z",
 * "correlationId": "abc-123",
 * "fieldErrors": null
 * }
 * }</pre>
 *
 * Uses Java 21 record for immutability and zero boilerplate.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String error,
        ErrorCode code,
        String message,
        Instant timestamp,
        String correlationId,
        Map<String, String> fieldErrors) {
    /** Convenience factory — no field errors, no correlationId. */
    public static ErrorResponse of(int status, String error, ErrorCode code, String message) {
        return new ErrorResponse(status, error, code, message, Instant.now(), null, null);
    }

    /** Convenience factory with correlationId tracking. */
    public static ErrorResponse of(int status, String error, ErrorCode code, String message, String correlationId) {
        return new ErrorResponse(status, error, code, message, Instant.now(), correlationId, null);
    }

    /** Factory for validation failures with per-field details. */
    public static ErrorResponse validationFailed(Map<String, String> fieldErrors, String correlationId) {
        return new ErrorResponse(400, "Validation Failed", ErrorCode.VALIDATION_FAILED,
                "One or more fields are invalid", Instant.now(), correlationId, fieldErrors);
    }
}
