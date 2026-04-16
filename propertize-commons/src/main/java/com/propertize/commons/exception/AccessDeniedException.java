package com.propertize.commons.exception;

/**
 * 403 — caller does not have permission to access this resource.
 */
public class AccessDeniedException extends BaseException {

    public AccessDeniedException(String message) {
        super(ErrorCode.ACCESS_DENIED, message);
    }

    public AccessDeniedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}

