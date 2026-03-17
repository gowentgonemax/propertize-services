package com.propertize.platform.employecraft.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Position/Job Title entity
 * Organization-scoped via organizationId from Propertize
 */
@Entity
@Table(name = "positions", indexes = {
    @Index(name = "idx_position_org", columnList = "organization_id"),
    @Index(name = "idx_position_code", columnList = "code, organization_id", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String code;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "min_salary")
    private java.math.BigDecimal minSalary;

    @Column(name = "max_salary")
    private java.math.BigDecimal maxSalary;

    @Column(name = "is_management")
    @Builder.Default
    private Boolean isManagement = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Version
    private Long version;
}
