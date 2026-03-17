package com.propertize.platform.auth.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    @JsonAlias({ "identifier", "usernameOrEmail", "email" })
    private String username;

    @NotBlank
    private String password;

    private Boolean rememberMe;
}
