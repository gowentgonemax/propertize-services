package com.propertize.payroll.exception;

/**
 * Exception thrown when validation fails.
 *
 * @author WageCraft Team
 * @version 1.0
 * @since 2026-02-05
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
