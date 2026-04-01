package com.propertize.platform.employecraft.exception;

import com.propertize.commons.exception.BaseException;
import com.propertize.commons.exception.ErrorCode;

/**
 * Exception for Propertize integration errors — extends shared BaseException.
 */
public class PropertizeIntegrationException extends BaseException {

    private final int statusCode;

    public PropertizeIntegrationException(String message, int statusCode) {
        super(ErrorCode.UPSTREAM_SERVICE_ERROR, message);
        this.statusCode = statusCode;
    }

    public PropertizeIntegrationException(String message, int statusCode, Throwable cause) {
        super(ErrorCode.UPSTREAM_SERVICE_ERROR, message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
