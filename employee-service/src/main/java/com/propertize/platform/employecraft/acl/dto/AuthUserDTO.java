package com.propertize.platform.employecraft.acl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * DTO representing a user from Auth Service, translated into
 * Employecraft's bounded context.
 * 
 * This is an ACL boundary object — it shields Employecraft from
 * changes in Auth Service's user model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserDTO {
    private UUID id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private UUID organizationId;
    private String organizationCode;
    private Set<String> roles;
    private boolean active;
}
