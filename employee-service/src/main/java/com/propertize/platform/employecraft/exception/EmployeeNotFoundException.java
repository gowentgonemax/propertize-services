package com.propertize.platform.employecraft.exception;

import java.util.UUID;

/**
 * Employee not found exception
 */
public class EmployeeNotFoundException extends RuntimeException {

    public EmployeeNotFoundException(UUID id) {
        super("Employee not found with id: " + id);
    }

    public EmployeeNotFoundException(String message) {
        super(message);
    }
}
