package com.propertize.platform.auth.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class UserDetails {
    private Long id;
    private String username;
    private String email;
    private String organizationId;
    private Set<String> roles;
}
