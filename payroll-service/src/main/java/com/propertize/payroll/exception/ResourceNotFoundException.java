package com.propertize.payroll.exception;

import com.propertize.commons.exception.BaseException;
import com.propertize.commons.exception.ErrorCode;

/**
 * Exception thrown when a requested resource is not found.
 *
 * @author WageCraft Team
 * @version 1.0
 * @since 2026-02-05
 */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message, cause);
    }

    public ResourceNotFoundException(String resource, String field, Object value) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
                String.format("%s not found with %s: %s", resource, field, value),
                resource, field, value);
    }
}
