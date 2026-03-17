package com.propertize.platform.auth.dto;

import com.propertize.enums.UserRoleEnum;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * DTO for updating user information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Email(message = "Email must be valid")
    private String email;

    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String organizationId;
    private Set<UserRoleEnum> roles;
    private Boolean enabled;
}
