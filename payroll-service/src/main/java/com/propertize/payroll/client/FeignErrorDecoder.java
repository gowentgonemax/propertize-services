package com.propertize.payroll.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Custom error decoder for Feign client errors
 */
@Slf4j
@Component
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("Feign client error - method: {}, status: {}, reason: {}",
            methodKey, response.status(), response.reason());

        switch (response.status()) {
            case 400:
                return new BadRequestException("Bad request to external service: " + response.reason());
            case 401:
                return new UnauthorizedException("Unauthorized access to external service");
            case 403:
                return new ForbiddenException("Forbidden access to external service");
            case 404:
                return new NotFoundException("Resource not found in external service");
            case 500:
            case 502:
            case 503:
            case 504:
                return new ServiceUnavailableException("External service unavailable: " + response.reason());
            default:
                return defaultDecoder.decode(methodKey, response);
        }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) { super(message); }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) { super(message); }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) { super(message); }
    }

    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException(String message) { super(message); }
    }
}
