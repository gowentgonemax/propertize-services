package com.propertize.platform.employecraft.config;

import com.propertize.platform.employecraft.security.filter.TrustedGatewayHeaderFilter.GatewayAuthenticatedUser;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Auditor aware implementation for JPA auditing.
 * Extracts the current username from the gateway-authenticated principal.
 */
@Component("auditorProvider")
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("system");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof GatewayAuthenticatedUser user) {
            String username = user.getUsername();
            return Optional.of(username != null ? username : user.getUserId());
        }

        return Optional.ofNullable(authentication.getName()).or(() -> Optional.of("system"));
    }
}
