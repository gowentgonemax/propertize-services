package com.propertize.commons.dto;

import com.propertize.commons.exception.BadRequestException;
import com.propertize.commons.exception.ConflictException;
import com.propertize.commons.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Unified Response Handler Utility — shared across all Propertize services.
 * <p>
 * Provides standardized methods for building {@link ApiResponse}-wrapped
 * {@link ResponseEntity} instances with consistent error handling.
 * <p>
 * Operation handlers ({@code handleFind}, {@code handleSave}, etc.) catch
 * commons exception types ({@link ResourceNotFoundException},
 * {@link BadRequestException}, {@link ConflictException}) and map them to
 * the appropriate HTTP status codes so controllers stay thin.
 */
@Slf4j
public final class ResponseHandler {

    private ResponseHandler() { /* utility class */ }

    // ==================== SUCCESS RESPONSES ====================

    /** 200 OK with data. */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /** 200 OK with data and custom message. */
    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    /** 200 OK with message only (no data payload). */
    public static <T> ResponseEntity<ApiResponse<T>> success(String message) {
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    /** 201 Created with data. */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Resource created successfully"));
    }

    /** 201 Created with data and custom message. */
    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, message));
    }

    /** 204 No Content. */
    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    // ==================== PAGINATION RESPONSES ====================

    /** 200 OK with paginated content and pagination metadata. */
    public static <T> ResponseEntity<ApiResponse<List<T>>> paginated(Page<T> page) {
        Map<String, Object> paginationMeta = Map.of(
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages(),
                "hasNext", page.hasNext(),
                "hasPrevious", page.hasPrevious(),
                "isFirst", page.isFirst(),
                "isLast", page.isLast()
        );

        return ResponseEntity.ok(
                ApiResponse.<List<T>>builder()
                        .success(true)
                        .data(page.getContent())
                        .message("Data retrieved successfully")
                        .metadata(paginationMeta)
                        .build()
        );
    }

    // ==================== ERROR RESPONSES ====================

    /** 400 Bad Request. */
    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.badRequest(message));
    }

    /** 404 Not Found. */
    public static <T> ResponseEntity<ApiResponse<T>> notFound(String resource) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.notFound(resource));
    }

    /** 403 Forbidden. */
    public static <T> ResponseEntity<ApiResponse<T>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.forbidden(message));
    }

    /** 401 Unauthorized. */
    public static <T> ResponseEntity<ApiResponse<T>> unauthorized(String message) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.unauthorized(message));
    }

    /** 409 Conflict. */
    public static <T> ResponseEntity<ApiResponse<T>> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.conflict(message));
    }

    /** 500 Internal Server Error. */
    public static <T> ResponseEntity<ApiResponse<T>> internalError(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(message));
    }

    // ==================== OPERATION HANDLERS ====================

    /**
     * Handle a find/read operation with standardised error mapping.
     *
     * @param operation    supplies the result (may return {@code null} for "not found")
     * @param resourceName human-readable resource label for messages
     */
    public static <T> ResponseEntity<ApiResponse<T>> handleFind(Supplier<T> operation, String resourceName) {
        try {
            T result = operation.get();
            if (result == null) {
                return notFound(resourceName);
            }
            return success(result, resourceName + " retrieved successfully");
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (Exception e) {
            log.error("Error finding {}: {}", resourceName, e.getMessage(), e);
            return internalError("Failed to retrieve " + resourceName.toLowerCase());
        }
    }

    /** Overload — infers resource name as "Resource". */
    public static <T> ResponseEntity<ApiResponse<T>> handleFind(Supplier<T> operation) {
        return handleFind(operation, "Resource");
    }

    /**
     * Handle a create/save operation with standardised error mapping.
     */
    public static <T> ResponseEntity<ApiResponse<T>> handleSave(Supplier<T> operation, String resourceName) {
        try {
            return created(operation.get(), resourceName + " created successfully");
        } catch (BadRequestException | IllegalArgumentException e) {
            log.warn("Validation error saving {}: {}", resourceName, e.getMessage());
            return badRequest(e.getMessage());
        } catch (ConflictException e) {
            log.warn("Conflict saving {}: {}", resourceName, e.getMessage());
            return conflict(e.getMessage());
        } catch (Exception e) {
            log.error("Error saving {}: {}", resourceName, e.getMessage(), e);
            return internalError("Failed to create " + resourceName.toLowerCase());
        }
    }

    /**
     * Handle an update operation with standardised error mapping.
     */
    public static <T> ResponseEntity<ApiResponse<T>> handleUpdate(Supplier<T> operation, String resourceName) {
        try {
            T result = operation.get();
            if (result == null) {
                return notFound(resourceName);
            }
            return success(result, resourceName + " updated successfully");
        } catch (BadRequestException | IllegalArgumentException e) {
            log.warn("Validation error updating {}: {}", resourceName, e.getMessage());
            return badRequest(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (ConflictException e) {
            log.warn("Conflict updating {}: {}", resourceName, e.getMessage());
            return conflict(e.getMessage());
        } catch (Exception e) {
            log.error("Error updating {}: {}", resourceName, e.getMessage(), e);
            return internalError("Failed to update " + resourceName.toLowerCase());
        }
    }

    /**
     * Handle a delete operation with standardised error mapping.
     */
    public static ResponseEntity<ApiResponse<Void>> handleDelete(Runnable operation, String resourceName) {
        try {
            operation.run();
            return ResponseEntity.ok(
                    ApiResponse.<Void>builder()
                            .success(true)
                            .message(resourceName + " deleted successfully")
                            .build()
            );
        } catch (ResourceNotFoundException e) {
            return notFound(e.getMessage());
        } catch (BadRequestException | IllegalArgumentException e) {
            log.warn("Validation error deleting {}: {}", resourceName, e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Error deleting {}: {}", resourceName, e.getMessage(), e);
            return internalError("Failed to delete " + resourceName.toLowerCase());
        }
    }

    /**
     * Handle a list operation with standardised error mapping.
     */
    public static <T> ResponseEntity<ApiResponse<List<T>>> handleList(Supplier<List<T>> operation, String resourceName) {
        try {
            return success(operation.get(), resourceName + " retrieved successfully");
        } catch (Exception e) {
            log.error("Error listing {}: {}", resourceName, e.getMessage(), e);
            return internalError("Failed to retrieve " + resourceName.toLowerCase());
        }
    }

    /**
     * Handle a paginated database operation with standardised error mapping.
     */
    public static <T> ResponseEntity<ApiResponse<List<T>>> handlePaginated(Supplier<Page<T>> operation, String resourceName) {
        try {
            return paginated(operation.get());
        } catch (Exception e) {
            log.error("Error retrieving paginated {}: {}", resourceName, e.getMessage(), e);
            return internalError("Failed to retrieve " + resourceName.toLowerCase());
        }
    }

    // ==================== UTILITY METHODS ====================

    /**
     * Wraps an exception in a sanitised 500 response — hides SQL/Hibernate/internal details.
     */
    public static <T> ResponseEntity<ApiResponse<T>> safeError(Exception e, String operation) {
        log.error("Error during {}: {}", operation, e.getMessage(), e);
        return internalError(getSafeErrorMessage(e, operation));
    }

    /**
     * Wraps an exception in a sanitised 400 response.
     * Shows the message for {@link IllegalArgumentException}/{@link IllegalStateException};
     * generic text otherwise.
     */
    public static <T> ResponseEntity<ApiResponse<T>> safeBadRequest(Exception e, String operation) {
        log.warn("Bad request during {}: {}", operation, e.getMessage());
        if (e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            return badRequest(e.getMessage());
        }
        return badRequest("Invalid request for " + operation);
    }

    /** Attach a correlation ID to an existing response. */
    public static <T> ResponseEntity<ApiResponse<T>> withCorrelationId(
            ResponseEntity<ApiResponse<T>> response, String correlationId) {
        if (response.getBody() != null) {
            response.getBody().setCorrelationId(correlationId);
        }
        return response;
    }

    /** Attach extra metadata to an existing response. */
    public static <T> ResponseEntity<ApiResponse<T>> withMetadata(
            ResponseEntity<ApiResponse<T>> response, Map<String, Object> metadata) {
        if (response.getBody() != null) {
            response.getBody().setMetadata(metadata);
        }
        return response;
    }

    // ── internal ──

    private static String getSafeErrorMessage(Exception e, String operation) {
        if (e.getMessage() != null &&
                (e.getMessage().contains("SQL") ||
                        e.getMessage().contains("jdbc") ||
                        e.getMessage().contains("Hibernate") ||
                        e.getMessage().contains("constraint") ||
                        e.getMessage().contains("Exception"))) {
            return "An error occurred during " + operation + ". Please try again later.";
        }
        return "Failed to complete " + operation + ". Please contact support if the issue persists.";
    }
}

