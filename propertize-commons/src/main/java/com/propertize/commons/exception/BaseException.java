package com.propertize.commons.exception;

import lombok.Getter;

/**
 * Root of the Propertize exception hierarchy.
 *
 * <p>
 * All domain exceptions should extend this class and supply an
 * {@link ErrorCode} so that the global handler can produce a consistent
 * {@link com.propertize.commons.dto.ErrorResponse} without any per-exception
 * handling logic.
 *
 * <pre>{@code
 * // Example domain exception
 * public class EmployeeNotFoundException extends BaseException {
 *     public EmployeeNotFoundException(Long id) {
 *         super(ErrorCode.EMPLOYEE_NOT_FOUND,
 *                 "Employee not found with id: " + id, id);
 *     }
 * }
 * }</pre>
 */
@Getter
public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    /** Optional structured context (IDs, field names, etc.) */
    private final Object[] context;

    protected BaseException(ErrorCode errorCode, String message, Object... context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context;
    }

    protected BaseException(ErrorCode errorCode, String message, Throwable cause, Object... context) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = context;
    }
}
