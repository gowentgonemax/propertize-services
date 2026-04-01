package com.propertize.platform.employecraft.exception;

import com.propertize.commons.dto.ErrorResponse;
import com.propertize.commons.exception.ErrorCode;
import com.propertize.commons.exception.PropertizeGlobalExceptionHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Employee-service exception handler.
 * Extends the shared {@link PropertizeGlobalExceptionHandler} from
 * propertize-commons;
 * only adds handling for service-specific exceptions not covered by the shared
 * base.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends PropertizeGlobalExceptionHandler {

    /**
     * Handles Propertize integration errors with dynamic status codes.
     */
    @ExceptionHandler(PropertizeIntegrationException.class)
    public ResponseEntity<ErrorResponse> handlePropertizeIntegration(PropertizeIntegrationException ex,
            HttpServletRequest req) {
        String cid = req.getHeader("X-Correlation-ID");
        if (cid == null || cid.isBlank())
            cid = java.util.UUID.randomUUID().toString();
        log.error("[{}] Propertize integration error: {}", cid, ex.getMessage());
        HttpStatus status = HttpStatus.valueOf(
                ex.getStatusCode() >= 400 && ex.getStatusCode() < 600 ? ex.getStatusCode() : 500);
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status.value(), "Integration Error",
                        ErrorCode.UPSTREAM_SERVICE_ERROR, ex.getMessage(), cid));
    }
}
