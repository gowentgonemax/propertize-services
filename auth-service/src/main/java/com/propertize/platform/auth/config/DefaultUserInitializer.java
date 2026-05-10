package com.propertize.platform.auth.config;

import com.propertize.commons.enums.UserRoleEnum;
import com.propertize.platform.auth.entity.User;
import com.propertize.platform.auth.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Default User Initializer
 *
 * Ensures the platform oversight account exists on every application startup.
 * The legacy admin account is removed so only the new superadmin credential is
 * maintained going forward.
 *
 * Override the default password via SUPERADMIN_DEFAULT_PASSWORD environment
 * variable.
 *
 * @author Propertize Platform
 * @since February 2026
 */
@Component
@Slf4j
public class DefaultUserInitializer implements ApplicationRunner {

    private static final String SUPERADMIN_USERNAME = "superadmin";
    private static final String SUPERADMIN_EMAIL = "superadmin@propertize.com";
    private static final String LEGACY_ADMIN_USERNAME = "admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String superadminDefaultPassword;

    public DefaultUserInitializer(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${superadmin.default-password:#{T(java.util.UUID).randomUUID().toString()}}") String superadminDefaultPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.superadminDefaultPassword = superadminDefaultPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("🔧 Initializing default superadmin user...");

        try {
            removeLegacyAdminUser();
            createDefaultUser(
                    SUPERADMIN_USERNAME,
                    SUPERADMIN_EMAIL,
                    superadminDefaultPassword,
                    "Super",
                    "Admin",
                    Set.of(UserRoleEnum.PLATFORM_OVERSIGHT));

            log.info("✅ Default superadmin user initialization completed successfully");
        } catch (Exception e) {
            log.error("❌ Failed to initialize default superadmin user", e);
        }
    }

    private void removeLegacyAdminUser() {
        userRepository.findByUsernameIgnoreCase(LEGACY_ADMIN_USERNAME)
                .ifPresent(user -> {
                    userRepository.delete(user);
                    log.info("🧹 Removed legacy admin user '{}'", LEGACY_ADMIN_USERNAME);
                });
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
