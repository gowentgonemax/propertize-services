package com.propertize.platform.auth.service;

import com.propertize.platform.auth.dto.*;
import com.propertize.platform.auth.entity.User;
import com.propertize.platform.auth.repository.UserRepository;
import com.propertize.platform.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsernameWithRoles(request.getUsername())
                .or(() -> userRepository.findByEmailWithRoles(request.getUsername()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.getEnabled()) {
            throw new BadCredentialsException("Account is disabled");
        }

        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900L)
                .userDetails(mapToUserDetails(user))
                .build();
    }

    public AuthResponse refresh(RefreshRequest request) {
        // Validate and refresh token logic
        return null; // Implement
    }

    @Transactional
    public LogoutResponse logout(LogoutRequest request) {
        // Blacklist tokens
        return new LogoutResponse("Logged out successfully", true);
    }

    private UserDetails mapToUserDetails(User user) {
        return UserDetails.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .organizationId(user.getOrganizationId())
                .roles(user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet()))
                .build();
    }
}
