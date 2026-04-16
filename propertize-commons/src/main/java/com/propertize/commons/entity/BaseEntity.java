package com.propertize.commons.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Shared base entity — provides optimistic locking and audit timestamps.
 * <p>
 * Note: {@code @Id} is intentionally omitted so each service can declare
 * its own primary key type (Long, UUID, String).
 * </p>
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity implements Serializable {

    @Version
    @Column(name = "version", nullable = true)
    private Long version;

    @PrePersist
    @PreUpdate
    protected void initializeVersion() {
        if (this.version == null) {
            this.version = 0L;
        }
    }

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;
}

