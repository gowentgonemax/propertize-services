package com.propertize.payment.config;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Helper to extract user/org context from JWT-forwarded headers.
 * The API Gateway validates the JWT and forwards user info via X-User-*
 * headers.
 */
@Component
@Getter
public class SecurityContext {

    public static String getCurrentUserId() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            String userId = request.getHeader("X-User-Id");
            if (userId != null && !userId.isBlank())
                return userId;
        }
        // fallback to Spring Security principal
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getUsername();
        }
        return null;
    }

    public static String getCurrentOrganizationId() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return request.getHeader("X-Organization-Id");
        }
        return null;
    }

    public static String getCurrentUserEmail() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return request.getHeader("X-User-Email");
        }
        return null;
    }

    public static String getCurrentUserRole() {
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            return request.getHeader("X-User-Role");
        }
        return null;
    }

    private static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                    .currentRequestAttributes();
            return attributes.getRequest();
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
