package com.propertize.platform.auth.controller;

import com.propertize.platform.auth.dto.*;
import com.propertize.platform.auth.entity.User;
import com.propertize.platform.auth.repository.UserCustomRoleAssignmentRepository;
import com.propertize.platform.auth.repository.UserRepository;
import com.propertize.platform.auth.security.JwtTokenProvider;
import com.propertize.platform.auth.service.*;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserCustomRoleAssignmentRepository userCustomRoleAssignmentRepository;
    @Mock
    private PasswordResetService passwordResetService;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private SessionManagementService sessionService;
    @Mock
    private RbacService rbacService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private PermissionCacheService permissionCacheService;

    @InjectMocks
    private AuthController authController;

    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getHeader("User-Agent")).thenReturn("test-agent");
        when(rateLimitService.isLoginAllowed(anyString())).thenReturn(true);
        when(rateLimitService.isIpAllowed(anyString())).thenReturn(true);
        when(rateLimitService.isPasswordResetAllowed(anyString())).thenReturn(true);
        when(userCustomRoleAssignmentRepository.findByUserIdAndIsActiveTrueWithRole(any()))
                .thenReturn(Collections.emptyList());
        when(rbacService.getExplicitDenialsForRoles(any())).thenReturn(Collections.emptySet());
        when(rbacService.getBasePermissionsForRole(anyString())).thenReturn(Set.of("tenant:view"));
        when(jwtTokenProvider.getJtiFromToken(anyString())).thenReturn("jti-abc");
    }

    private LoginRequest loginRequest(String user, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(user);
        req.setPassword(password);
        return req;
    }

    private User buildUser(String username) {
        User u = new User();
        u.setId(1L);
        u.setUsername(username);
        u.setEmail(username + "@test.com");
        u.setFirstName("John");
        u.setLastName("Doe");
        u.setOrganizationId("org-1");
        u.setOrganizationCode("ORG01");
        u.setRoles(Collections.emptySet());
        return u;
    }

    // ── POST /login ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class Login {

        @Test
        @DisplayName("Returns 429 when login rate limit is exceeded")
        void returns429WhenRateLimitExceeded() {
            when(rateLimitService.isLoginAllowed("jdoe")).thenReturn(false);

            ResponseEntity<AuthResponse> response = authController.login(loginRequest("jdoe", "pass"), httpRequest);

            assertThat(response.getStatusCode().value()).isEqualTo(429);
        }

        @Test
        @DisplayName("Returns 429 when IP rate limit is exceeded")
        void returns429WhenIpRateLimitExceeded() {
            when(rateLimitService.isIpAllowed("127.0.0.1")).thenReturn(false);

            ResponseEntity<AuthResponse> response = authController.login(loginRequest("jdoe", "pass"), httpRequest);

            assertThat(response.getStatusCode().value()).isEqualTo(429);
        }

        @Test
        @DisplayName("Returns 401 on bad credentials")
        void returns401OnBadCredentials() {
            when(userRepository.findByEmail("jdoe")).thenReturn(Optional.empty());
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("wrong password"));

            ResponseEntity<AuthResponse> response = authController.login(loginRequest("jdoe", "wrong"), httpRequest);

            assertThat(response.getStatusCode().value()).isEqualTo(401);
            verify(rateLimitService).recordFailedLogin("jdoe");
        }

        @Test
        @DisplayName("Returns 401 on disabled account")
        void returns401OnDisabledAccount() {
            when(userRepository.findByEmail("jdoe")).thenReturn(Optional.empty());
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new DisabledException("account disabled"));

            ResponseEntity<AuthResponse> response = authController.login(loginRequest("jdoe", "pass"), httpRequest);

            assertThat(response.getStatusCode().value()).isEqualTo(401);
        }

        @Test
        @DisplayName("Returns 200 with tokens on successful login")
        void returns200OnSuccess() {
            GrantedAuthority adminAuth = () -> "ROLE_ADMIN";
            when(authentication.getName()).thenReturn("jdoe");
            when(authentication.getAuthorities())
                    .thenReturn((Collection) List.of(adminAuth));
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(userRepository.findByEmail("jdoe")).thenReturn(Optional.empty());
            User u = buildUser("jdoe");
            when(userRepository.findByUsernameWithRoles("jdoe")).thenReturn(Optional.of(u));
            when(jwtTokenProvider.generateAccessTokenWithPermissions(
                    anyString(), any(), anyString(), anyString(), any(), any(), anyString(), anyString()))
                    .thenReturn("access-token-xyz");
            when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-token-xyz");
            UserSessionInfo si = UserSessionInfo.builder()
                    .sessionId("ses-1").username("jdoe").isActive(true).build();
            when(sessionService.createSession(any(), any(), any(), any())).thenReturn(si);

            ResponseEntity<AuthResponse> response = authController.login(loginRequest("jdoe", "correct"), httpRequest);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody().getAccessToken()).isEqualTo("access-token-xyz");
            assertThat(response.getBody().getRefreshToken()).isEqualTo("refresh-token-xyz");
        }

        @Test
        @DisplayName("Resolves username from email when identifier is an email address")
        void resolvesUsernameFromEmail() {
            User emailUser = buildUser("jdoe");
            when(userRepository.findByEmail("jdoe@test.com")).thenReturn(Optional.of(emailUser));
            GrantedAuthority roleAuth = () -> "ROLE_ADMIN";
            when(authentication.getName()).thenReturn("jdoe");
            when(authentication.getAuthorities()).thenReturn((Collection) List.of(roleAuth));
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(userRepository.findByUsernameWithRoles("jdoe")).thenReturn(Optional.of(emailUser));
            when(jwtTokenProvider.generateAccessTokenWithPermissions(
                    anyString(), any(), anyString(), anyString(), any(), any(), anyString(), anyString()))
                    .thenReturn("tok");
            when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("rtok");
            when(sessionService.createSession(any(), any(), any(), any()))
                    .thenReturn(UserSessionInfo.builder().sessionId("s1").username("jdoe").build());

            ResponseEntity<AuthResponse> response = authController.login(loginRequest("jdoe@test.com", "pass"),
                    httpRequest);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            verify(authenticationManager).authenticate(
                    argThat(a -> a instanceof UsernamePasswordAuthenticationToken &&
                            "jdoe".equals(((UsernamePasswordAuthenticationToken) a).getPrincipal())));
        }
    }

    // ── POST /logout ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/logout")
    class Logout {

        @Test
        @DisplayName("Returns 200 with success message")
        void returns200OnLogout() {
            when(jwtTokenProvider.getUsernameFromToken("tok")).thenReturn("jdoe");
            when(jwtTokenProvider.getJtiFromToken("tok")).thenReturn("jti-1");

            ResponseEntity<Map<String, Object>> response = authController.logout("Bearer tok", null, httpRequest);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("message", "Logged out successfully");
        }

        @Test
        @DisplayName("Blacklists access token on logout")
        void blacklistsAccessToken() {
            when(jwtTokenProvider.getUsernameFromToken("tok")).thenReturn("jdoe");
            when(jwtTokenProvider.getJtiFromToken("tok")).thenReturn("jti-1");

            authController.logout("Bearer tok", null, httpRequest);

            verify(tokenBlacklistService).blacklistToken("tok", 86400);
            verify(permissionCacheService).evictPermissions("jti-1");
        }

        @Test
        @DisplayName("Returns 200 even when no Authorization header is present")
        void returns200WithNoAuthHeader() {
            ResponseEntity<Map<String, Object>> response = authController.logout(null, null, httpRequest);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
        }
    }

    // ── POST /forgot-password ─────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password")
    class ForgotPassword {

        @Test
        @DisplayName("Returns 429 when rate limit exceeded")
        void returns429WhenRateLimited() {
            when(rateLimitService.isPasswordResetAllowed("user@test.com")).thenReturn(false);
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("user@test.com");

            ResponseEntity<Map<String, Object>> response = authController.forgotPassword(req, httpRequest);

            assertThat(response.getStatusCode().value()).isEqualTo(429);
        }

        @Test
        @DisplayName("Returns 200 and triggers password reset regardless of email existence")
        void returns200AndDelegates() {
            ForgotPasswordRequest req = new ForgotPasswordRequest();
            req.setEmail("anyone@test.com");
            doNothing().when(passwordResetService).forgotPassword("anyone@test.com");

            ResponseEntity<Map<String, Object>> response = authController.forgotPassword(req, httpRequest);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("success", true);
            verify(passwordResetService).forgotPassword("anyone@test.com");
        }
    }

    // ── POST /reset-password ──────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPassword {

        @Test
        @DisplayName("Returns 200 on successful password reset")
        void returns200OnSuccess() {
            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setToken("valid-token");
            req.setNewPassword("Str0ng!Pass");
            req.setConfirmPassword("Str0ng!Pass");
            doNothing().when(passwordResetService).resetPassword(eq("valid-token"), eq("Str0ng!Pass"),
                    eq("Str0ng!Pass"));

            ResponseEntity<Map<String, Object>> response = authController.resetPassword(req);

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("success", true);
        }

        @Test
        @DisplayName("Returns 400 when reset throws IllegalArgumentException (e.g. password mismatch)")
        void returns400OnValidationError() {
            ResetPasswordRequest req = new ResetPasswordRequest();
            req.setToken("tok");
            req.setNewPassword("abc");
            req.setConfirmPassword("xyz");
            doThrow(new IllegalArgumentException("Passwords do not match"))
                    .when(passwordResetService).resetPassword("tok", "abc", "xyz");

            ResponseEntity<Map<String, Object>> response = authController.resetPassword(req);

            assertThat(response.getStatusCode().value()).isEqualTo(400);
            assertThat(response.getBody()).containsEntry("success", false);
        }
    }

    // ── GET /validate-reset-token ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/auth/validate-reset-token")
    class ValidateResetToken {

        @Test
        @DisplayName("Returns valid=true for a valid token")
        void returnsValidTrue() {
            when(passwordResetService.validateToken("good-token")).thenReturn(true);
            when(passwordResetService.getUserEmailByToken("good-token"))
                    .thenReturn(Optional.of("user@test.com"));

            ResponseEntity<Map<String, Object>> response = authController.validateResetToken("good-token");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("valid", true);
        }

        @Test
        @DisplayName("Returns valid=false and error message for an invalid token")
        void returnsValidFalse() {
            when(passwordResetService.validateToken("bad-token")).thenReturn(false);

            ResponseEntity<Map<String, Object>> response = authController.validateResetToken("bad-token");

            assertThat(response.getStatusCode().value()).isEqualTo(200);
            assertThat(response.getBody()).containsEntry("valid", false);
            assertThat(response.getBody().get("message"))
                    .isEqualTo("Token is invalid or expired");
        }
    }
}
