package com.propertize.platform.auth.service;

import com.propertize.platform.auth.entity.PasswordResetToken;
import com.propertize.platform.auth.entity.User;
import com.propertize.platform.auth.repository.PasswordResetTokenRepository;
import com.propertize.platform.auth.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PasswordResetService Tests")
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private User buildUser(Long id, String email, String username) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setUsername(username);
        u.setPassword("old-hashed");
        return u;
    }

    private PasswordResetToken validToken(Long userId) {
        return PasswordResetToken.builder()
                .token("abc123")
                .userId(userId)
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();
    }

    // ── forgotPassword ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("forgotPassword()")
    class ForgotPassword {

        @Test
        @DisplayName("Should save reset token when user exists")
        void savesTokenForExistingUser() {
            User user = buildUser(1L, "test@example.com", "testuser");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            passwordResetService.forgotPassword("test@example.com");

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(1L);
            assertThat(captor.getValue().getToken()).isNotBlank();
            assertThat(captor.getValue().getUsed()).isFalse();
            assertThat(captor.getValue().getExpiryDate()).isAfter(LocalDateTime.now());
        }

        @Test
        @DisplayName("Should do nothing when user does not exist")
        void doesNothingForMissingUser() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            passwordResetService.forgotPassword("ghost@example.com");

            verify(tokenRepository, never()).save(any());
        }
    }

    // ── resetPassword ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("resetPassword()")
    class ResetPassword {

        @Test
        @DisplayName("Should reset password when token is valid")
        void resetsPasswordSuccessfully() {
            User user = buildUser(5L, "user@example.com", "user");
            PasswordResetToken token = validToken(5L);
            when(tokenRepository.findByToken("abc123")).thenReturn(Optional.of(token));
            when(userRepository.findById(5L)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("NewPass1!")).thenReturn("new-hashed");
            when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            passwordResetService.resetPassword("abc123", "NewPass1!", "NewPass1!");

            assertThat(user.getPassword()).isEqualTo("new-hashed");
            assertThat(token.getUsed()).isTrue();
            verify(tokenRepository).deleteByUserId(5L);
        }

        @Test
        @DisplayName("Should throw when passwords do not match")
        void throwsWhenPasswordsMismatch() {
            assertThatThrownBy(() -> passwordResetService.resetPassword("tok", "Pass1!", "Pass2!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("do not match");
            verify(tokenRepository, never()).findByToken(any());
        }

        @Test
        @DisplayName("Should throw when token is not found")
        void throwsForUnknownToken() {
            when(tokenRepository.findByToken("bad")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.resetPassword("bad", "P@ss1!", "P@ss1!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid reset token");
        }

        @Test
        @DisplayName("Should throw when token is expired")
        void throwsForExpiredToken() {
            PasswordResetToken expired = PasswordResetToken.builder()
                    .token("exp")
                    .userId(1L)
                    .expiryDate(LocalDateTime.now().minusHours(1))
                    .used(false)
                    .build();
            when(tokenRepository.findByToken("exp")).thenReturn(Optional.of(expired));

            assertThatThrownBy(() -> passwordResetService.resetPassword("exp", "P@ss1!", "P@ss1!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("Should throw when token is already used")
        void throwsForUsedToken() {
            PasswordResetToken used = validToken(2L);
            used.setUsed(true);
            when(tokenRepository.findByToken("used")).thenReturn(Optional.of(used));

            assertThatThrownBy(() -> passwordResetService.resetPassword("used", "P@ss1!", "P@ss1!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already been used");
        }

        @Test
        @DisplayName("Should throw when user not found for valid token")
        void throwsWhenUserNotFound() {
            PasswordResetToken token = validToken(99L);
            when(tokenRepository.findByToken("abc123")).thenReturn(Optional.of(token));
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> passwordResetService.resetPassword("abc123", "P@ss1!", "P@ss1!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // ── validateToken ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateToken()")
    class ValidateToken {

        @Test
        @DisplayName("Returns true for valid non-expired unused token")
        void returnsTrueForValidToken() {
            when(tokenRepository.findByToken("good")).thenReturn(Optional.of(validToken(1L)));
            assertThat(passwordResetService.validateToken("good")).isTrue();
        }

        @Test
        @DisplayName("Returns false when token not found")
        void returnsFalseForMissingToken() {
            when(tokenRepository.findByToken("missing")).thenReturn(Optional.empty());
            assertThat(passwordResetService.validateToken("missing")).isFalse();
        }

        @Test
        @DisplayName("Returns false for expired token")
        void returnsFalseForExpiredToken() {
            PasswordResetToken expired = PasswordResetToken.builder()
                    .token("exp").userId(1L)
                    .expiryDate(LocalDateTime.now().minusMinutes(1))
                    .used(false).build();
            when(tokenRepository.findByToken("exp")).thenReturn(Optional.of(expired));
            assertThat(passwordResetService.validateToken("exp")).isFalse();
        }

        @Test
        @DisplayName("Returns false for used token")
        void returnsFalseForUsedToken() {
            PasswordResetToken used = validToken(1L);
            used.setUsed(true);
            when(tokenRepository.findByToken("used")).thenReturn(Optional.of(used));
            assertThat(passwordResetService.validateToken("used")).isFalse();
        }
    }

    // ── getUserEmailByToken ──────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserEmailByToken()")
    class GetUserEmailByToken {

        @Test
        @DisplayName("Returns email for valid token with existing user")
        void returnsEmail() {
            PasswordResetToken token = validToken(3L);
            User user = buildUser(3L, "owner@test.com", "owner");
            when(tokenRepository.findByToken("tok")).thenReturn(Optional.of(token));
            when(userRepository.findById(3L)).thenReturn(Optional.of(user));

            assertThat(passwordResetService.getUserEmailByToken("tok"))
                    .isPresent().hasValue("owner@test.com");
        }

        @Test
        @DisplayName("Returns empty for expired token")
        void returnsEmptyForExpiredToken() {
            PasswordResetToken expired = PasswordResetToken.builder()
                    .token("exp").userId(1L)
                    .expiryDate(LocalDateTime.now().minusMinutes(5))
                    .used(false).build();
            when(tokenRepository.findByToken("exp")).thenReturn(Optional.of(expired));

            assertThat(passwordResetService.getUserEmailByToken("exp")).isEmpty();
        }
    }
}
