package com.propertize.payment.entity.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class OrganizationScopedEntity extends AuditableEntity {

    @Column(name = "organization_id", nullable = false, length = 36)
    private String organizationId;

    protected void validateOrganizationId() {
        if (organizationId == null || organizationId.isBlank()) {
            throw new IllegalStateException(
                    "organizationId must be set for " + this.getClass().getSimpleName());
        }
    }
}
