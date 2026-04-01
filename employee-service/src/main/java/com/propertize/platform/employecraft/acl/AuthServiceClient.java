package com.propertize.platform.employecraft.acl;

import com.propertize.platform.employecraft.acl.dto.AuthUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for Auth Service communication.
 * Part of the Anti-Corruption Layer — only used by
 * AuthServiceAntiCorruptionLayer.
 */
@FeignClient(name = "auth-service", url = "${services.auth.url:http://localhost:8081}", path = "/api/v1/users")
public interface AuthServiceClient {

    @GetMapping("/{userId}")
    AuthUserDTO getUserById(@PathVariable("userId") String userId);
}
