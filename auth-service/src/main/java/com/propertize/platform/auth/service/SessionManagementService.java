package com.propertize.platform.auth.service;

import com.propertize.platform.auth.dto.UserSessionInfo;
import com.propertize.platform.auth.util.HttpRequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Session Management Service
 * Handles session creation and tracking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementService {

    private static final String SESSION_ATTR_USER_INFO = "USER_SESSION_INFO";
    private static final String SESSION_ATTR_USERNAME = "USERNAME";
    private static final String SESSION_ATTR_ORGANIZATION_ID = "ORGANIZATION_ID";
    private static final String SESSION_ATTR_ORGANIZATION_CODE = "ORGANIZATION_CODE";

    private final RedisTemplate<String, Object> redisTemplate;

    public UserSessionInfo createSession(HttpServletRequest request,
            Authentication authentication,
            String organizationId,
            String organizationCode) {
        try {
            HttpSession session = request.getSession(true);

            String username = authentication.getName();
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            UserSessionInfo sessionInfo = UserSessionInfo.builder()
                    .sessionId(session.getId())
                    .username(username)
                    .organizationId(organizationId)
                    .organizationCode(organizationCode)
                    .roles(roles)
                    .createdAt(Instant.now())
                    .lastAccessedAt(Instant.now())
                    .ipAddress(HttpRequestUtil.getClientIpAddress(request))
                    .userAgent(request.getHeader("User-Agent"))
                    .isActive(true)
                    .build();

            session.setAttribute(SESSION_ATTR_USER_INFO, sessionInfo);
            session.setAttribute(SESSION_ATTR_USERNAME, username);
            session.setAttribute(SESSION_ATTR_ORGANIZATION_ID, organizationId);
            session.setAttribute(SESSION_ATTR_ORGANIZATION_CODE, organizationCode);

            try {
                String sessionKey = "user:session:" + username + ":" + session.getId();
                redisTemplate.opsForValue().set(sessionKey, sessionInfo, 1, TimeUnit.HOURS);
                log.debug("Session stored in Redis: {}", sessionKey);
            } catch (Exception e) {
                log.warn("Redis session storage failed (session still valid): {}", e.getMessage());
            }

            log.info("✅ Session created for user: {}", username);
            return sessionInfo;

        } catch (Exception e) {
            log.error("❌ Failed to create session: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create session", e);
        }
    }

    public void invalidateSession(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                String username = (String) session.getAttribute(SESSION_ATTR_USERNAME);
                session.invalidate();

                if (username != null) {
                    try {
                        String sessionKey = "user:session:" + username + ":*";
                        redisTemplate.delete(sessionKey);
                    } catch (Exception e) {
                        log.warn("Redis session cleanup failed: {}", e.getMessage());
                    }
                }

                log.info("Session invalidated for user: {}", username);
            }
        } catch (Exception e) {
            log.error("Failed to invalidate session: {}", e.getMessage());
        }
    }
}
