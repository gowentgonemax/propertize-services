package com.propertize.payment.entity.listener;

import com.propertize.payment.entity.base.AuditableEntity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuditListener {

    @PrePersist
    public void prePersist(AuditableEntity entity) {
        String user = getCurrentUsername();
        entity.setCreatedBy(user);
        entity.setUpdatedBy(user);
    }

    @PreUpdate
    public void preUpdate(AuditableEntity entity) {
        entity.setUpdatedBy(getCurrentUsername());
    }

    private String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception ignored) {
        }
        return "system";
    }
}
