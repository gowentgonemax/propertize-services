package com.propertize.platform.auth.service;

import com.propertize.enums.UserRoleEnum;
import com.propertize.platform.auth.dto.CreateUserRequest;
import com.propertize.platform.auth.dto.UpdateUserRequest;
import com.propertize.platform.auth.dto.UserInfoResponse;
import com.propertize.platform.auth.entity.User;
import com.propertize.platform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserInfoResponse createUser(CreateUserRequest request) {
        log.info("Creating user: {}", request.getUsername());

        // Check if user already exists
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }

        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .organizationId(request.getOrganizationId())
                .organizationCode(request.getOrganizationCode())
                .roles(request.getRoles() != null ? request.getRoles() : new HashSet<>())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        user = userRepository.save(user);
        log.info("✅ User created successfully: {} (ID: {})", user.getUsername(), user.getId());

        return mapToUserInfoResponse(user);
    }

    @Transactional
    public UserInfoResponse updateUser(Long userId, UpdateUserRequest request) {
        log.info("Updating user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Update fields if provided
        if (request.getEmail() != null) {
            // Check email uniqueness
            userRepository.findByEmail(request.getEmail()).ifPresent(existing -> {
                if (!existing.getId().equals(userId)) {
                    throw new IllegalArgumentException("Email already exists: " + request.getEmail());
                }
            });
            user.setEmail(request.getEmail());
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getOrganizationId() != null) {
            user.setOrganizationId(request.getOrganizationId());
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            user.setRoles(request.getRoles());
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        user = userRepository.save(user);
        log.info("✅ User updated successfully: {}", userId);

        return mapToUserInfoResponse(user);
    }

    public UserInfoResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        return mapToUserInfoResponse(user);
    }

    public UserInfoResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        return mapToUserInfoResponse(user);
    }

    public UserInfoResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        return mapToUserInfoResponse(user);
    }

    /**
     * List users with pagination and optional organization filter.
     * Organization members only see users in their own organization.
     * Platform admins can see all users (pass null organizationId).
     */
    public Page<UserInfoResponse> getAllUsers(String organizationId, Pageable pageable) {
        Page<User> users;
        if (organizationId != null && !organizationId.isBlank()) {
            users = userRepository.findByOrganizationId(organizationId, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(this::mapToUserInfoResponse);
    }

    @Transactional
    public void updatePassword(Long userId, String newPassword) {
        log.info("Updating password for user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("✅ Password updated for user: {}", userId);
    }

    private UserInfoResponse mapToUserInfoResponse(User user) {
        Set<String> roleNames = new HashSet<>();
        if (user.getRoles() != null) {
            user.getRoles().forEach(role -> roleNames.add(role.name()));
        }

        return UserInfoResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .organizationId(user.getOrganizationId())
                .roles(roleNames)
                .enabled(user.getEnabled())
                .accountNonLocked(user.isAccountNonLocked())
                .build();
    }
}
