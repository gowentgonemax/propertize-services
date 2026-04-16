package com.propertize.payment.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Payment-service specific ResourceNotFoundException.
 * Extends commons ResourceNotFoundException so shared ResponseHandler and
 * GlobalExceptionHandler correctly return HTTP 404.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends com.propertize.commons.exception.ResourceNotFoundException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String entity, String field, Object value) {
        super(entity, field, value);
    }
}
