package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a department within a client organization.
 */
@Entity
@Table(name = "departments", indexes = {
    @Index(name = "idx_department_client", columnList = "client_id"),
    @Index(name = "idx_department_code", columnList = "departmentCode"),
    @Index(name = "idx_department_parent", columnList = "parent_department_id")
})
@Getter
@Setter
public class DepartmentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String departmentCode;

    @Column(length = 500)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_department_id")
    private DepartmentEntity parentDepartment;

    @OneToMany(mappedBy = "parentDepartment")
    private List<DepartmentEntity> childDepartments = new ArrayList<>();

    /**
     * Cost center code for this department
     */
    @Column(length = 20)
    private String costCenter;

    /**
     * GL account code
     */
    @Column(length = 20)
    private String glAccountCode;

    /**
     * Department manager (reference to employee)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private EmployeeEntity manager;

    /**
     * Whether department is active
     */
    @Column(nullable = false)
    private Boolean isActive = true;

    /**
     * Default work location for this department
     */
    @Column(length = 100)
    private String defaultWorkLocation;

    /**
     * State where department is located (for tax purposes)
     */
    @Column(length = 2)
    private String stateCode;
}
