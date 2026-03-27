package com.propertize.platform.employecraft.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Kafka event published when an employee is created, updated, or terminated.
 * Consumed by payroll-service to keep its local cache in sync.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeEvent implements Serializable {

    public enum EventType {
        CREATED, UPDATED, ACTIVATED, TERMINATED
    }

    private EventType eventType;
    private UUID employeeId;
    private UUID organizationId;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String status;
    private String employmentType;
    private LocalDate hireDate;
    private LocalDate terminationDate;

    // Compensation snapshot
    private String payType;
    private BigDecimal payRate;
    private String payFrequency;

    private LocalDateTime occurredAt;
}
