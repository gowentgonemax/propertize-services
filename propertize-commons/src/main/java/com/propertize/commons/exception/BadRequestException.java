package com.propertize.commons.exception;

/** 400 — the request is malformed or contains invalid data. */
public class BadRequestException extends BaseException {

    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }

    public BadRequestException(String message, Object... context) {
        super(ErrorCode.BAD_REQUEST, message, context);
    }
}
