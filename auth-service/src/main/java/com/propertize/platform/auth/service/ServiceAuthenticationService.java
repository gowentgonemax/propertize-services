package com.propertize.platform.auth.service;

import com.propertize.platform.auth.dto.ServiceTokenRequest;
import com.propertize.platform.auth.dto.ServiceTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceAuthenticationService {

    public ServiceTokenResponse generateServiceToken(ServiceTokenRequest request) {
        // Service-to-service authentication
        return ServiceTokenResponse.builder()
                .serviceToken("service-token")
                .expiresIn(300L)
                .build();
    }
}
