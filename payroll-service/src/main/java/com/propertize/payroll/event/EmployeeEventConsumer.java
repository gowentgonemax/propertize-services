package com.propertize.payroll.event;

import com.propertize.payroll.entity.Client;
import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.payroll.enums.EmployeeStatusEnum;
import com.propertize.payroll.enums.EmploymentTypeEnum;
import com.propertize.payroll.enums.PayFrequencyEnum;
import com.propertize.payroll.enums.PayTypeEnum;
import com.propertize.payroll.repository.ClientRepository;
import com.propertize.payroll.repository.EmployeeEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Consumes employee lifecycle events from Kafka and updates the local payroll
 * employee cache.
 * Replaces polling-based sync with near-real-time event-driven sync.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeEventConsumer {

    private final EmployeeEntityRepository employeeRepository;
    private final ClientRepository clientRepository;

    @KafkaListener(topics = "employee-events", groupId = "payroll-service")
    @Transactional
    @CacheEvict(value = "employees", key = "#event.employeeId", condition = "#event.employeeId != null")
    public void handleEmployeeEvent(EmployeeEvent event) {
        log.info("Received {} event for employee {} ({})",
                event.getEventType(), event.getEmployeeId(), event.getEmployeeNumber());

        try {
            switch (event.getEventType()) {
                case CREATED, UPDATED, ACTIVATED -> upsertEmployee(event);
                case TERMINATED -> handleTermination(event);
            }
        } catch (Exception e) {
            log.error("Failed to process {} event for employee {}: {}",
                    event.getEventType(), event.getEmployeeId(), e.getMessage(), e);
        }
    }

    private void upsertEmployee(EmployeeEvent event) {
        // Look up the client by organizationId
        Optional<Client> clientOpt = clientRepository.findByOrganizationId(event.getOrganizationId());
        if (clientOpt.isEmpty()) {
            log.warn("No client found for organizationId {} — skipping employee event", event.getOrganizationId());
            return;
        }

        EmployeeEntity employee = employeeRepository
                .findByExternalEmployeeId(event.getEmployeeId())
                .orElse(new EmployeeEntity());

        employee.setClient(clientOpt.get());
        employee.setExternalEmployeeId(event.getEmployeeId());
        employee.setEmployeeNumber(event.getEmployeeNumber());
        employee.setFirstName(event.getFirstName());
        employee.setLastName(event.getLastName());
        employee.setHireDate(event.getHireDate());
        employee.setTerminationDate(event.getTerminationDate());

        if (event.getStatus() != null) {
            try {
                employee.setStatus(EmployeeStatusEnum.valueOf(event.getStatus()));
            } catch (IllegalArgumentException e) {
                employee.setStatus(EmployeeStatusEnum.ACTIVE);
            }
        }

        if (event.getEmploymentType() != null) {
            try {
                employee.setEmploymentType(EmploymentTypeEnum.valueOf(event.getEmploymentType()));
            } catch (IllegalArgumentException e) {
                employee.setEmploymentType(EmploymentTypeEnum.FULL_TIME);
            }
        }

        if (event.getPayType() != null) {
            try {
                employee.setPayType(PayTypeEnum.valueOf(event.getPayType()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (event.getPayFrequency() != null) {
            try {
                employee.setPayFrequency(PayFrequencyEnum.valueOf(event.getPayFrequency()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (event.getPayRate() != null) {
            employee.setHourlyRate(event.getPayRate());
        }

        employeeRepository.save(employee);
        log.info("Synced employee {} via Kafka event", event.getEmployeeNumber());
    }

    private void handleTermination(EmployeeEvent event) {
        employeeRepository.findByExternalEmployeeId(event.getEmployeeId())
                .ifPresentOrElse(employee -> {
                    employee.setStatus(EmployeeStatusEnum.TERMINATED);
                    employee.setTerminationDate(event.getTerminationDate());
                    employeeRepository.save(employee);
                    log.info("Terminated local employee {} via Kafka event", event.getEmployeeNumber());
                }, () -> log.warn("Employee {} not found locally for termination event", event.getEmployeeId()));
    }
}
