package com.propertize.platform.employecraft.client;

import com.propertize.platform.employecraft.exception.PropertizeIntegrationException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom error decoder for Propertize Feign client
 */
public class FeignErrorDecoder implements ErrorDecoder {

    private static final Logger logger = LoggerFactory.getLogger(FeignErrorDecoder.class);
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String requestUrl = response.request().url();

        logger.error("Feign client error: method={}, status={}, url={}", methodKey, status, requestUrl);

        return switch (status) {
            case 400 -> new PropertizeIntegrationException("Bad request to Propertize: " + requestUrl, status);
            case 401 -> new PropertizeIntegrationException("Unauthorized access to Propertize", status);
            case 403 -> new PropertizeIntegrationException("Forbidden access to Propertize resource", status);
            case 404 -> new PropertizeIntegrationException("Resource not found in Propertize: " + requestUrl, status);
            case 409 -> new PropertizeIntegrationException("Conflict in Propertize: " + requestUrl, status);
            case 500, 502, 503, 504 -> new PropertizeIntegrationException("Propertize service unavailable", status);
            default -> defaultDecoder.decode(methodKey, response);
        };
    }
}
