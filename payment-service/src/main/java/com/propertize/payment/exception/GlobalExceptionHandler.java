package com.propertize.payment.exception;

import com.propertize.commons.exception.PropertizeGlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Payment-service exception handler.
 * Delegates all standard exception handling to the shared
 * {@link PropertizeGlobalExceptionHandler} from propertize-commons.
 * Add any payment-service-specific handlers here.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GlobalExceptionHandler extends PropertizeGlobalExceptionHandler {
    // All handlers (ResourceNotFoundException, BadRequestException,
    // MethodArgumentNotValidException, etc.) are inherited from PropertizeGlobalExceptionHandler.
    // Add payment-specific @ExceptionHandler methods here if needed.
}
