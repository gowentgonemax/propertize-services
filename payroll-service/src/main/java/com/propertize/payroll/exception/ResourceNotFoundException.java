package com.propertize.payroll.exception;

/**
 * Exception thrown when a requested resource is not found.
 *
 * @author WageCraft Team
 * @version 1.0
 * @since 2026-02-05
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(String.format("%s not found with %s: %s", resource, field, value));
    }
}
