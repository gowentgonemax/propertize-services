package com.propertize.platform.auth.controller;

import com.propertize.platform.auth.dto.*;
import com.propertize.platform.auth.entity.User;
import com.propertize.platform.auth.repository.UserRepository;
import com.propertize.platform.auth.security.JwtTokenProvider;
import com.propertize.platform.auth.service.*;
import com.propertize.platform.auth.util.HttpRequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enhanced Authentication Controller
 * Features: Login, Password Reset, Session Management, RBAC, Rate Limiting
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordResetService passwordResetService;
    private final RateLimitService rateLimitService;
    private final SessionManagementService sessionService;
    private final RbacService rbacService;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        final String identifier = request.getUsername();
        final String ipAddress = HttpRequestUtil.getClientIpAddress(httpRequest);

        log.info("Login attempt for identifier: {} from IP: {}", identifier, ipAddress);

        // Rate limiting
        if (!rateLimitService.isLoginAllowed(identifier)) {
            log.warn("Rate limit exceeded for: {}", identifier);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        if (!rateLimitService.isIpAllowed(ipAddress)) {
            log.warn("IP rate limit exceeded: {}", ipAddress);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        // Resolve username from email if needed
        final String username;
        var userByEmail = userRepository.findByEmail(identifier);
        if (userByEmail.isPresent()) {
            username = userByEmail.get().getUsername();
        } else {
            username = identifier;
        }

        try {
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));

            log.info("🔐 Authentication successful for user: {}", authentication.getName());

            // Reset failed attempts on successful login
            rateLimitService.resetFailedAttempts(username);

            // Extract roles
            Set<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .map(auth -> auth.startsWith("ROLE_") ? auth.substring(5) : auth)
                    .collect(Collectors.toSet());

            // Load user with organization info
            var userOpt = userRepository.findByUsernameWithRoles(username);
            if (userOpt.isEmpty()) {
                throw new BadCredentialsException("User not found");
            }

            User user = userOpt.get();

            // Ensure roles from DB
            if (roles.isEmpty() && user.getRoles() != null && !user.getRoles().isEmpty()) {
                user.getRoles().forEach(role -> roles.add(role.name()));
            }

            if (roles.isEmpty()) {
                log.error("❌ User {} has no roles assigned", username);
                throw new org.springframework.security.access.AccessDeniedException(
                        "User has no roles assigned. Please contact administrator.");
            }

            // Get organization info from user's organizationIds
            String organizationId = (user.getOrganizationIds() != null && !user.getOrganizationIds().isEmpty())
                    ? user.getOrganizationIds().get(0)
                    : null;
            String organizationCode = organizationId; // Use organizationId as code for now

            // Create session
            String sessionId = null;
            try {
                UserSessionInfo sessionInfo = sessionService.createSession(
                        httpRequest, authentication,
                        organizationId,
                        organizationCode);
                sessionId = sessionInfo.getSessionId();
            } catch (Exception e) {
                log.warn("Session creation failed: {}", e.getMessage());
            }

            // Collect BASE permissions for JWT (no expansion - keep token small)
            // Permission hierarchy will be expanded during permission checks, not in JWT
            Set<String> permissions = roles.stream()
                    .flatMap(role -> rbacService.getBasePermissionsForRole(role).stream())
                    .collect(Collectors.toSet());

            log.info("📋 Collected {} base permissions for JWT token (user: {})", permissions.size(), username);

            // Generate tokens
            String accessToken = jwtTokenProvider.generateAccessTokenWithPermissions(
                    username, roles, organizationId,
                    organizationCode, permissions);
            String refreshToken = jwtTokenProvider.generateRefreshToken(user);

            log.info("✅ User '{}' logged in successfully with roles: {}", username, roles);

            return ResponseEntity.ok(AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(900L)
                    .username(username)
                    .roles(roles)
                    .sessionId(sessionId)
                    .build());

        } catch (BadCredentialsException e) {
            rateLimitService.recordFailedLogin(username);
            log.warn("❌ Login failed for: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        log.info("Token refresh attempt");

        try {
            String username = jwtTokenProvider.getUsernameFromToken(request.getRefreshToken());

            if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            var userOpt = userRepository.findByUsernameWithRoles(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            User user = userOpt.get();
            Set<String> roles = user.getRoles().stream()
                    .map(role -> role.name())
                    .collect(Collectors.toSet());

            // Get organization info from user's organizationIds
            String organizationId = (user.getOrganizationIds() != null && !user.getOrganizationIds().isEmpty())
                    ? user.getOrganizationIds().get(0)
                    : null;
            String organizationCode = organizationId; // Use organizationId as code for now

            Set<String> permissions = roles.stream()
                    .flatMap(role -> rbacService.getBasePermissionsForRole(role).stream())
                    .collect(Collectors.toSet());

            String newAccessToken = jwtTokenProvider.generateAccessTokenWithPermissions(
                    username, roles, organizationId,
                    organizationCode, permissions);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

            log.info("✅ Token refreshed for user: {}", username);

            return ResponseEntity.ok(AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(900L)
                    .username(username)
                    .roles(roles)
                    .build());

        } catch (Exception e) {
            log.warn("❌ Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, String> requestBody,
            HttpServletRequest httpRequest) {

        String username = "unknown";
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                username = jwtTokenProvider.getUsernameFromToken(token);
                tokenBlacklistService.blacklistToken(token, 86400); // 24 hours
            } catch (Exception e) {
                log.warn("Failed to extract username from token: {}", e.getMessage());
            }
        }

        sessionService.invalidateSession(httpRequest);

        String reason = requestBody != null && requestBody.containsKey("reason")
                ? requestBody.get("reason")
                : "manual";

        log.info("✅ User '{}' logged out successfully (reason: {})", username, reason);

        return ResponseEntity.ok(Map.of(
                "message", "Logged out successfully",
                "success", true,
                "reason", reason));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {

        String email = request.getEmail();
        String ipAddress = HttpRequestUtil.getClientIpAddress(httpRequest);

        log.info("Password reset requested for email: {} from IP: {}", email, ipAddress);

        if (!rateLimitService.isPasswordResetAllowed(email)) {
            log.warn("Password reset rate limit exceeded for: {}", email);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many reset attempts. Please try again later.", "success", false));
        }

        passwordResetService.forgotPassword(email);

        return ResponseEntity.ok(Map.of(
                "message", "If the email exists in our system, a password reset link has been sent",
                "success", true));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Password reset attempt with token");

        try {
            passwordResetService.resetPassword(request.getToken(), request.getNewPassword(),
                    request.getConfirmPassword());

            log.info("✅ Password reset successful");

            return ResponseEntity.ok(Map.of(
                    "message", "Password has been reset successfully",
                    "success", true));

        } catch (IllegalArgumentException e) {
            log.warn("Password reset failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage(),
                    "success", false));
        }
    }

    @GetMapping("/validate-reset-token")
    public ResponseEntity<Map<String, Object>> validateResetToken(@RequestParam String token) {
        log.info("Token validation requested");

        boolean valid = passwordResetService.validateToken(token);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", valid);

        if (valid) {
            passwordResetService.getUserEmailByToken(token)
                    .ifPresent(email -> response.put("email", email));
            response.put("message", "Token is valid");
        } else {
            response.put("message", "Token is invalid or expired");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "message", "Authentication required",
                    "success", false));
        }

        String username = authentication.getName();
        log.info("🔐 Password change request for user: {}", username);

        try {
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "New password and confirmation do not match",
                        "success", false));
            }

            var userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "message", "User not found",
                        "success", false));
            }

            User user = userOpt.get();

            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Current password is incorrect",
                        "success", false));
            }

            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            userRepository.save(user);

            log.info("✅ Password changed successfully for user: {}", username);

            return ResponseEntity.ok(Map.of(
                    "message", "Password changed successfully",
                    "success", true));

        } catch (Exception e) {
            log.error("❌ Password change failed for user {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Failed to change password",
                    "success", false));
        }
    }
}
