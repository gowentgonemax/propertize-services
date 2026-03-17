package com.propertize.platform.employecraft.exception;

/**
 * Exception for Propertize integration errors
 */
public class PropertizeIntegrationException extends RuntimeException {

    private final int statusCode;

    public PropertizeIntegrationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public PropertizeIntegrationException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
