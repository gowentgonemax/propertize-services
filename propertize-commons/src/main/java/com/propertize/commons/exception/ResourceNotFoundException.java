package com.propertize.commons.exception;

/** 404 — the requested resource was not found. */
public class ResourceNotFoundException extends BaseException {

    public ResourceNotFoundException(String resourceType, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
                resourceType + " not found with id: " + id,
                resourceType, id);
    }

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    /** 3-arg constructor: entity not found with field = value. */
    public ResourceNotFoundException(String entity, String field, Object value) {
        super(ErrorCode.RESOURCE_NOT_FOUND,
                entity + " not found with " + field + ": " + value,
                entity, field, value);
    }
}
