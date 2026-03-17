package com.propertize.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class ServiceTokenResponse {
    private String serviceToken;
    private Long expiresIn;
}
