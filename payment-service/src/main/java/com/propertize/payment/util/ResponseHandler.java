package com.propertize.payment.util;

import com.propertize.payment.dto.common.ApiResponse;
import com.propertize.payment.exception.BadRequestException;
import com.propertize.payment.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Slf4j
public class ResponseHandler {

    public static <T> ResponseEntity<ApiResponse<T>> success(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(data, message));
    }

    public static <T> ResponseEntity<ApiResponse<List<T>>> paginated(Page<T> page) {
        Map<String, Object> meta = Map.of(
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages(),
                "hasNext", page.hasNext(),
                "hasPrevious", page.hasPrevious());
        return ResponseEntity.ok(ApiResponse.<List<T>>builder()
                .success(true)
                .data(page.getContent())
                .message("Data retrieved successfully")
                .metadata(meta)
                .build());
    }

    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return ResponseEntity.badRequest().body(ApiResponse.badRequest(message));
    }

    public static <T> ResponseEntity<ApiResponse<T>> internalError(String message) {
        return ResponseEntity.internalServerError().body(ApiResponse.error(message));
    }

    public static <T> ResponseEntity<ApiResponse<T>> handleSave(Supplier<T> op, String name) {
        try {
            return created(op.get(), name + " created successfully");
        } catch (BadRequestException | IllegalArgumentException e) {
            log.warn("Validation saving {}: {}", name, e.getMessage());
            return badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Error saving {}: {}", name, e.getMessage(), e);
            return internalError("Failed to create " + name.toLowerCase());
        }
    }

    public static <T> ResponseEntity<ApiResponse<T>> handleFind(Supplier<T> op, String name) {
        try {
            T result = op.get();
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound(name));
            }
            return success(result, name + " retrieved successfully");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            log.error("Error finding {}: {}", name, e.getMessage(), e);
            return internalError("Failed to retrieve " + name.toLowerCase());
        }
    }

    public static <T> ResponseEntity<ApiResponse<T>> handleUpdate(Supplier<T> op, String name) {
        try {
            T result = op.get();
            if (result == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound(name));
            }
            return success(result, name + " updated successfully");
        } catch (BadRequestException | IllegalArgumentException e) {
            log.warn("Validation updating {}: {}", name, e.getMessage());
            return badRequest(e.getMessage());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating {}: {}", name, e.getMessage(), e);
            return internalError("Failed to update " + name.toLowerCase());
        }
    }

    public static ResponseEntity<ApiResponse<Void>> handleDelete(Runnable op, String name) {
        try {
            op.run();
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true).message(name + " deleted successfully").build());
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting {}: {}", name, e.getMessage(), e);
            return internalError("Failed to delete " + name.toLowerCase());
        }
    }

    public static <T> ResponseEntity<ApiResponse<List<T>>> handleList(Supplier<List<T>> op, String name) {
        try {
            return success(op.get(), name + " retrieved successfully");
        } catch (Exception e) {
            log.error("Error listing {}: {}", name, e.getMessage(), e);
            return internalError("Failed to retrieve " + name.toLowerCase());
        }
    }

    public static <T> ResponseEntity<ApiResponse<List<T>>> handlePaginated(Supplier<Page<T>> op, String name) {
        try {
            return paginated(op.get());
        } catch (Exception e) {
            log.error("Error paginating {}: {}", name, e.getMessage(), e);
            return internalError("Failed to retrieve " + name.toLowerCase());
        }
    }
}
