package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.entity.embedded.Address;
import com.propertize.payroll.entity.embedded.ContactInfo;
import com.propertize.payroll.enums.ClientStatusEnum;
import com.propertize.payroll.enums.PayrollScheduleEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a client/company that uses the payroll service.
 * Links to Propertize organization for multi-tenant integration.
 */
@Entity
@Table(name = "clients", indexes = {
    @Index(name = "idx_client_tax_id", columnList = "taxId"),
    @Index(name = "idx_client_status", columnList = "status"),
    @Index(name = "idx_client_organization", columnList = "organization_id")
})
@Getter
@Setter
@BatchSize(size = 25)
public class Client extends BaseEntity {

    /**
     * Organization ID from Propertize - required for multi-tenancy
     */
    @Column(name = "organization_id")
    private UUID organizationId;

    /**
     * Service account token for Employecraft API calls
     * Used for background sync operations
     */
    @Column(name = "service_account_token", length = 2000)
    private String serviceAccountToken;

    /**
     * When the service account token expires
     */
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(nullable = false)
    private String companyName;

    @Column(unique = true, nullable = false, length = 20)
    private String taxId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street", column = @Column(name = "company_street")),
        @AttributeOverride(name = "city", column = @Column(name = "company_city")),
        @AttributeOverride(name = "state", column = @Column(name = "company_state")),
        @AttributeOverride(name = "zipCode", column = @Column(name = "company_zip_code")),
        @AttributeOverride(name = "country", column = @Column(name = "company_country")),
        @AttributeOverride(name = "addressType", column = @Column(name = "company_address_type"))
    })
    private Address companyAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "email", column = @Column(name = "primary_email", nullable = false)),
        @AttributeOverride(name = "phone", column = @Column(name = "primary_phone")),
        @AttributeOverride(name = "mobile", column = @Column(name = "primary_mobile")),
        @AttributeOverride(name = "fax", column = @Column(name = "primary_fax")),
        @AttributeOverride(name = "contactPerson", column = @Column(name = "primary_contact_person"))
    })
    private ContactInfo contactInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClientStatusEnum status = ClientStatusEnum.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayrollScheduleEnum payrollSchedule;

    @Column(length = 50)
    private String industry;

    @Column
    private Integer employeeCount;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PayrollRun> payrollRuns = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BenefitPlan> benefitPlans = new ArrayList<>();
}
