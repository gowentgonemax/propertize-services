package com.propertize.platform.auth.dto;

import com.propertize.enums.UserRoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO for creating a new user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String firstName;
    private String lastName;
    private String phoneNumber;

    @NotNull(message = "Organization ID is required")
    private String organizationId;

    private String organizationCode;

    @NotNull(message = "At least one role is required")
    private Set<UserRoleEnum> roles;

    @Builder.Default
    private Boolean enabled = true;
}
