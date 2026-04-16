package com.propertize.platform.auth.service;

import com.propertize.platform.auth.dto.UserSessionInfo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SessionManagementService Tests")
class SessionManagementServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOps;

    @InjectMocks
    private SessionManagementService sessionManagementService;

    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private HttpSession httpSession;
    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── createSession() ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("createSession()")
    class CreateSession {

        @BeforeEach
        void wire() {
            when(httpRequest.getSession(true)).thenReturn(httpSession);
            when(httpSession.getId()).thenReturn("session-abc-123");
            when(authentication.getName()).thenReturn("jdoe");
            GrantedAuthority roleAdmin = () -> "ROLE_ADMIN";
            when(authentication.getAuthorities())
                    .thenReturn((Collection) List.of(roleAdmin));
            when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
            when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
            when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        }

        @Test
        @DisplayName("Returns UserSessionInfo with correct username and session ID")
        void returnsSessionInfoWithUsername() {
            UserSessionInfo info = sessionManagementService.createSession(
                    httpRequest, authentication, "org-1", "ORG01");

            assertThat(info.getUsername()).isEqualTo("jdoe");
            assertThat(info.getSessionId()).isEqualTo("session-abc-123");
            assertThat(info.getOrganizationId()).isEqualTo("org-1");
            assertThat(info.getOrganizationCode()).isEqualTo("ORG01");
            assertThat(info.isActive()).isTrue();
        }

        @Test
        @DisplayName("Populates roles from authentication authorities")
        void populatesRoles() {
            UserSessionInfo info = sessionManagementService.createSession(
                    httpRequest, authentication, "org-1", "ORG01");

            assertThat(info.getRoles()).containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Stores session attributes on HttpSession")
        void storesSessionAttributes() {
            sessionManagementService.createSession(
                    httpRequest, authentication, "org-2", "ORG02");

            verify(httpSession).setAttribute(eq("USER_SESSION_INFO"), any(UserSessionInfo.class));
            verify(httpSession).setAttribute(eq("USERNAME"), eq("jdoe"));
            verify(httpSession).setAttribute(eq("ORGANIZATION_ID"), eq("org-2"));
            verify(httpSession).setAttribute(eq("ORGANIZATION_CODE"), eq("ORG02"));
        }

        @Test
        @DisplayName("Writes session to Redis with TTL")
        void writesSessionToRedis() {
            sessionManagementService.createSession(
                    httpRequest, authentication, "org-1", "ORG01");

            verify(valueOps).set(contains("jdoe"), any(UserSessionInfo.class), eq(1L), any());
        }

        @Test
        @DisplayName("Still returns session when Redis throws (non-fatal)")
        void sessionSurvivesRedisFault() {
            when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis down"));

            UserSessionInfo info = sessionManagementService.createSession(
                    httpRequest, authentication, "org-1", "ORG01");

            assertThat(info).isNotNull();
            assertThat(info.getUsername()).isEqualTo("jdoe");
        }
    }

    // ── invalidateSession() ───────────────────────────────────────────────────

    @Nested
    @DisplayName("invalidateSession()")
    class InvalidateSession {

        @Test
        @DisplayName("Invalidates the HTTP session")
        void invalidatesHttpSession() {
            when(httpRequest.getSession(false)).thenReturn(httpSession);
            when(httpSession.getAttribute("USERNAME")).thenReturn("jdoe");

            sessionManagementService.invalidateSession(httpRequest);

            verify(httpSession).invalidate();
        }

        @Test
        @DisplayName("Attempts Redis cleanup using username key")
        void attemptsRedisCleanup() {
            when(httpRequest.getSession(false)).thenReturn(httpSession);
            when(httpSession.getAttribute("USERNAME")).thenReturn("jdoe");

            sessionManagementService.invalidateSession(httpRequest);

            verify(redisTemplate).delete("user:session:jdoe:*");
        }

        @Test
        @DisplayName("Does nothing when session is null")
        void noopWhenNoSession() {
            when(httpRequest.getSession(false)).thenReturn(null);

            assertThatCode(() -> sessionManagementService.invalidateSession(httpRequest))
                    .doesNotThrowAnyException();

            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("Skips Redis delete when username is null")
        void skipsRedisWhenUsernameNull() {
            when(httpRequest.getSession(false)).thenReturn(httpSession);
            when(httpSession.getAttribute("USERNAME")).thenReturn(null);

            sessionManagementService.invalidateSession(httpRequest);

            verify(httpSession).invalidate();
            verify(redisTemplate, never()).delete(anyString());
        }

        @Test
        @DisplayName("Handles Redis failure gracefully during invalidation")
        void handlesRedisFailureGracefully() {
            when(httpRequest.getSession(false)).thenReturn(httpSession);
            when(httpSession.getAttribute("USERNAME")).thenReturn("jdoe");
            when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("Redis down"));

            assertThatCode(() -> sessionManagementService.invalidateSession(httpRequest))
                    .doesNotThrowAnyException();
        }
    }
}
