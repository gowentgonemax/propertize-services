package com.propertize.commons.dto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.function.Supplier;

/**
 * Utility class for building standard {@link ResponseEntity} responses.
 * Methods wrap supplier calls in try/catch and map exceptions to HTTP status codes.
 */
public final class ResponseHandler {

    private ResponseHandler() {}

    /** 200 OK — return data directly. */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /** 200 OK — return data with message. */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    /** 200 OK — return message only. */
    public static <T> ResponseEntity<ApiResponse<T>> success(String message) {
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /** 201 Created — return data with message. */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(data, message));
    }

    /**
     * Execute supplier and wrap result in 200 OK; on exception return 500.
     * Suitable for list queries.
     */
    public static <T> ResponseEntity<ApiResponse<List<T>>> handleList(Supplier<List<T>> supplier, String resourceName) {
        try {
            List<T> result = supplier.get();
            return ResponseEntity.ok(ApiResponse.success(result, resourceName + " list retrieved successfully"));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ex.getMessage()));
        }
    }

    /**
     * Execute supplier and wrap result in 200 OK; on exception return 404 or 500.
     * Suitable for single-item lookups.
     */
    public static <T> ResponseEntity<ApiResponse<T>> handleFind(Supplier<T> supplier, String resourceName) {
        try {
            T result = supplier.get();
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(resourceName + " not found"));
            }
            return ResponseEntity.ok(ApiResponse.success(result, resourceName + " retrieved successfully"));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ex.getMessage()));
        }
    }

    /**
     * Execute supplier and wrap result in 201 Created; on exception return 500.
     * Suitable for create operations.
     */
    public static <T> ResponseEntity<ApiResponse<T>> handleSave(Supplier<T> supplier, String resourceName) {
        try {
            T result = supplier.get();
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(result, resourceName + " created successfully"));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ex.getMessage()));
        }
    }

    /**
     * Execute supplier and wrap result in 200 OK; on exception return 500.
     * Suitable for update operations.
     */
    public static <T> ResponseEntity<ApiResponse<T>> handleUpdate(Supplier<T> supplier, String resourceName) {
        try {
            T result = supplier.get();
            return ResponseEntity.ok(ApiResponse.success(result, resourceName + " updated successfully"));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ex.getMessage()));
        }
    }

    /**
     * Execute supplier and return 204 No Content; on exception return 500.
     * Suitable for delete operations.
     */
    public static <T> ResponseEntity<ApiResponse<T>> handleDelete(Runnable action, String resourceName) {
        try {
            action.run();
            return ResponseEntity.ok(ApiResponse.success(resourceName + " deleted successfully"));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ex.getMessage()));
        }
    }
}
