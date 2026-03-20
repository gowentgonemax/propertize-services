package com.propertize.platform.auth.config;

import com.propertize.platform.auth.entity.User;
import com.propertize.enums.UserRoleEnum;
import com.propertize.platform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Default User Initializer
 * 
 * Creates a single admin user on application startup if it doesn't exist.
 * This ensures there's always at least one admin user available for system
 * access.
 * 
 * User credentials:
 * - Username: admin
 * - Password: password (CHANGE IN PRODUCTION!)
 * - Role: PLATFORM_OVERSIGHT
 * 
 * @author Propertize Platform
 * @since February 2026
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("🔧 Initializing default admin user...");

        try {
            // Create single admin user
            createDefaultUser(
                    "admin",
                    "admin@propertize.com",
                    "password",
                    "Admin",
                    "User",
                    Set.of(UserRoleEnum.PLATFORM_OVERSIGHT));

            log.info("✅ Default admin user initialization completed successfully");
        } catch (Exception e) {
            log.error("❌ Failed to initialize default admin user", e);
        }
    }

    /**
     * Create a default user if it doesn't already exist
     * 
     * @param username  The username
     * @param email     The email address
     * @param password  The plaintext password (will be encoded)
     * @param firstName The first name
     * @param lastName  The last name
     * @param roles     The set of roles to assign
     */
    private void createDefaultUser(
            String username,
            String email,
            String password,
            String firstName,
            String lastName,
            Set<UserRoleEnum> roles) {

        // Check if user already exists
        if (userRepository.existsByUsername(username)) {
            log.info("ℹ️ User '{}' already exists - skipping creation", username);
            return;
        }

        if (userRepository.existsByEmail(email)) {
            log.info("ℹ️ User with email '{}' already exists - skipping creation", email);
            return;
        }

        // Create new user
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .roles(roles)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        userRepository.save(user);

        log.info("✅ Created default user: {}", username);
        log.info("   Username: {}", username);
        log.info("   Email: {}", email);
        log.info("   Roles: {}", roles);
    }
}
