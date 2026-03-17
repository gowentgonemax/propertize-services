package com.propertize.platform.auth.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * Event published when a user is created, updated, or deleted in auth-service.
 * Consumed by propertize-service to maintain user profile synchronization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {

    public enum EventType {
        USER_CREATED,
        USER_UPDATED,
        USER_DELETED,
        USER_ENABLED,
        USER_DISABLED,
        USER_LOCKED,
        USER_UNLOCKED,
        PASSWORD_CHANGED,
        ROLES_UPDATED
    }

    private String eventId;
    private EventType eventType;
    private Instant timestamp;
    private String correlationId;

    // User data
    private Long userId;
    private String username;
    private String email;
    private Boolean enabled;
    private Boolean accountNonExpired;
    private Boolean accountNonLocked;
    private Boolean credentialsNonExpired;
    private Set<String> roles;

    // Metadata
    private String initiatedBy; // Username who initiated the change
    private String ipAddress;
    private String reason; // Reason for the change (if applicable)
}
