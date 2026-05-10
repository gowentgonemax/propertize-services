package com.propertize.platform.auth.dto;

import com.propertize.commons.enums.UserRoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@$!%*?&])(?=\\S+$).{8,}$", message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character (@$!%*?&)")
    private String password;

    private String firstName;
    private String lastName;
    private String phoneNumber;

    @NotNull(message = "Organization ID is required")
    private String organizationId;

    private String organizationCode;

    /** Denormalized org-type so users get org-type in JWT on first login. */
    private String organizationType;

    @NotNull(message = "At least one role is required")
    private Set<UserRoleEnum> roles;

    @Builder.Default
    private Boolean enabled = true;
}
