package com.propertize.payroll.event;

import com.propertize.commons.enums.employee.EmployeeStatusEnum;

import com.propertize.commons.enums.employee.PayFrequencyEnum;

import com.propertize.commons.enums.employee.PayTypeEnum;

import com.propertize.commons.enums.employee.EmploymentTypeEnum;

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
 * Kafka event received from employee-service.
 * Mirror of com.propertize.platform.employecraft.event.EmployeeEvent.
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
    private EmployeeStatusEnum status;
    private EmploymentTypeEnum employmentType;
    private LocalDate hireDate;
    private LocalDate terminationDate;

    // Compensation snapshot
    private PayTypeEnum payType;
    private BigDecimal payRate;
    private PayFrequencyEnum payFrequency;

    private LocalDateTime occurredAt;
}
