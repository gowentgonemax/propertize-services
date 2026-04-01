package com.propertize.payroll.exception;

import com.propertize.commons.exception.PropertizeGlobalExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Payroll-service exception handler.
 * Inherits all handlers from {@link PropertizeGlobalExceptionHandler}
 * (BaseException,
 * MethodArgumentNotValidException, DataIntegrityViolation, etc.).
 * Add payroll-specific @ExceptionHandler methods here if needed.
 */
@RestControllerAdvice
@Order(0)
@Slf4j
public class GlobalExceptionHandler extends PropertizeGlobalExceptionHandler {
}
