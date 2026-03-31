package com.propertize.payment.exception;

import com.propertize.commons.exception.BaseException;
import com.propertize.commons.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends BaseException {
    public BadRequestException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }
}
