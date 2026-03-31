package com.propertize.commons.exception;

/** 502/503 — a downstream service call failed. */
public class UpstreamServiceException extends BaseException {

    public UpstreamServiceException(String serviceName, String reason) {
        super(ErrorCode.UPSTREAM_SERVICE_ERROR,
                "Call to " + serviceName + " failed: " + reason,
                serviceName);
    }

    public UpstreamServiceException(String serviceName, Throwable cause) {
        super(ErrorCode.UPSTREAM_SERVICE_ERROR,
                "Call to " + serviceName + " failed: " + cause.getMessage(),
                cause,
                serviceName);
    }
}
