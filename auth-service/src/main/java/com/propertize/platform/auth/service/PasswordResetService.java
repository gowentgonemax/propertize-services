package com.propertize.platform.auth.service;

import com.propertize.platform.auth.entity.PasswordResetToken;
import com.propertize.platform.auth.entity.User;
import com.propertize.platform.auth.repository.PasswordResetTokenRepository;
import com.propertize.platform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for password reset functionality
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.frontend.reset-password-path:/reset-password}")
    private String resetPasswordPath;

    @Transactional
    public void forgotPassword(String email) {
        log.info("Password reset requested for email: {}", email);

        Optional<User> userOpt = userRepository.findByEmail(email);

        userOpt.ifPresent(user -> {
            String token = generateToken();

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(token)
                    .userId(user.getId())
                    .expiryDate(LocalDateTime.now().plusHours(24))
                    .used(false)
                    .build();

            tokenRepository.save(resetToken);

            // TODO: Send email via email service
            String resetLink = frontendUrl + resetPasswordPath + "?token=" + token;
            log.info("Password reset link for {}: {}", email, resetLink);
        });

        if (userOpt.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {}", email);
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        log.info("Password reset attempt with token");

        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid reset token");
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("Reset token has expired");
        }

        if (Boolean.TRUE.equals(resetToken.getUsed())) {
            throw new IllegalArgumentException("Reset token has already been used");
        }

        Optional<User> userOpt = userRepository.findById(resetToken.getUserId());

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("✅ Password reset successful for user: {}", user.getUsername());
    }

    public boolean validateToken(String token) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        return !resetToken.isExpired() && !Boolean.TRUE.equals(resetToken.getUsed());
    }

    public Optional<String> getUserEmailByToken(String token) {
        return tokenRepository.findByToken(token)
                .filter(t -> !t.isExpired() && !Boolean.TRUE.equals(t.getUsed()))
                .flatMap(t -> userRepository.findById(t.getUserId()))
                .map(User::getEmail);
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM daily
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Cleaning up expired password reset tokens");
        tokenRepository.deleteByExpiryDateBefore(LocalDateTime.now());
    }
}
