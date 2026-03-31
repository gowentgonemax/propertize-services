package com.propertize.payment.exception;

import com.propertize.commons.exception.BaseException;
import com.propertize.commons.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends BaseException {
    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(String entity, String field, Object value) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
                entity + " not found with " + field + ": " + value,
                entity, field, value);
    }
}
