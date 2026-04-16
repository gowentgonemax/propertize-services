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
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class GlobalExceptionHandler extends PropertizeGlobalExceptionHandler {
    // Payment ResourceNotFoundException extends commons ResourceNotFoundException,
    // so it is handled automatically by the inherited handleNotFound method.
}
