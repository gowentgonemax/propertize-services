package com.propertize.platform.employecraft.exception;

import com.propertize.commons.exception.BaseException;
import com.propertize.commons.exception.ErrorCode;

import java.util.UUID;

/**
 * Employee not found exception — extends shared BaseException hierarchy.
 */
public class EmployeeNotFoundException extends BaseException {

    public EmployeeNotFoundException(UUID id) {
        super(ErrorCode.EMPLOYEE_NOT_FOUND, "Employee not found with id: " + id);
    }

    public EmployeeNotFoundException(String message) {
        super(ErrorCode.EMPLOYEE_NOT_FOUND, message);
    }
}
