package com.propertize.payment.entity.base;

import com.propertize.payment.entity.listener.AuditListener;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AuditableEntity extends BaseEntity {

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    @Column(name = "deletion_reason", length = 500)
    private String deletionReason;

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void markAsDeleted(String deletedBy, String reason) {
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
        this.deletionReason = reason;
    }

    public void restore() {
        this.deletedAt = null;
        this.deletedBy = null;
        this.deletionReason = null;
    }
}
