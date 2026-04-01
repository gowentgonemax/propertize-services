package com.propertize.payroll.exception;

import com.propertize.commons.exception.BaseException;
import com.propertize.commons.exception.ErrorCode;

/**
 * Exception thrown when validation fails.
 *
 * @author WageCraft Team
 * @version 1.0
 * @since 2026-02-05
 */
public class ValidationException extends BaseException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
    }

    public ValidationException(String message, Throwable cause) {
        super(ErrorCode.VALIDATION_FAILED, message, cause);
    }
}
