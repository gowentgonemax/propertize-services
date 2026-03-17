package com.propertize.platform.auth.controller;

import com.propertize.platform.auth.dto.CreateUserRequest;
import com.propertize.platform.auth.dto.UpdateUserRequest;
import com.propertize.platform.auth.dto.UserInfoResponse;
import com.propertize.platform.auth.service.UserManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User Management Controller
 * 
 * Provides REST API endpoints for user management operations.
 * Used by other services (propertize-service) for inter-service user
 * operations.
 * 
 * Endpoints:
 * - POST /api/v1/users - Create new user
 * - GET /api/v1/users/{id} - Get user by ID
 * - GET /api/v1/users/username/{username} - Get user by username
 * - GET /api/v1/users/email/{email} - Get user by email
 * - PUT /api/v1/users/{id} - Update user
 * 
 * @author Propertize Platform Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserManagementController {

    private final UserManagementService userManagementService;

    /**
     * Create a new user.
     * 
     * @param request User creation request
     * @return Created user information
     */
    @PostMapping
    public ResponseEntity<UserInfoResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("API: Creating user '{}'", request.getUsername());

        try {
            UserInfoResponse response = userManagementService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.error("Failed to create user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user by ID.
     * 
     * @param id User ID
     * @return User information
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserInfoResponse> getUserById(@PathVariable Long id) {
        log.debug("API: Getting user by ID: {}", id);

        try {
            UserInfoResponse response = userManagementService.getUserById(id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("User not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get user by username.
     * 
     * @param username Username
     * @return User information
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserInfoResponse> getUserByUsername(@PathVariable String username) {
        log.debug("API: Getting user by username: {}", username);

        try {
            UserInfoResponse response = userManagementService.getUserByUsername(username);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("User not found: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get user by email.
     * 
     * @param email User email
     * @return User information
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserInfoResponse> getUserByEmail(@PathVariable String email) {
        log.debug("API: Getting user by email: {}", email);

        try {
            UserInfoResponse response = userManagementService.getUserByEmail(email);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("User not found with email: {}", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Update user information.
     * 
     * @param id      User ID
     * @param request Update request
     * @return Updated user information
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserInfoResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("API: Updating user: {}", id);

        try {
            UserInfoResponse response = userManagementService.updateUser(id, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.error("Failed to update user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (RuntimeException e) {
            log.error("User not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
