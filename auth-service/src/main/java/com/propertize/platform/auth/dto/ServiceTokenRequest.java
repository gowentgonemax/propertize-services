package com.propertize.platform.auth.dto;

import lombok.Data;

@Data
public class ServiceTokenRequest {
    private String serviceName;
    private String serviceSecret;
}
