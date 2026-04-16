package com.propertize.platform.employecraft.event;

import com.propertize.commons.constants.KafkaTopics;
import com.propertize.platform.employecraft.entity.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Publishes employee lifecycle events to the
 * {@value KafkaTopics#EMPLOYEE_EVENTS} Kafka topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeEventPublisher {

    /**
     * Canonical topic name — use {@link KafkaTopics#EMPLOYEE_EVENTS} in new code.
     */
    public static final String TOPIC = KafkaTopics.EMPLOYEE_EVENTS;

    private final KafkaTemplate<String, EmployeeEvent> kafkaTemplate;

    public void publish(Employee employee, EmployeeEvent.EventType eventType) {
        EmployeeEvent.EmployeeEventBuilder builder = EmployeeEvent.builder()
                .eventType(eventType)
                .employeeId(employee.getId())
                .organizationId(employee.getOrganizationId())
                .employeeNumber(employee.getEmployeeNumber())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .status(employee.getStatus())
                .employmentType(employee.getEmploymentType())
                .hireDate(employee.getHireDate())
                .terminationDate(employee.getTerminationDate())
                .occurredAt(LocalDateTime.now());

        if (employee.getCompensation() != null) {
            var comp = employee.getCompensation();
            builder.payType(comp.getPayType())
                    .payRate(comp.getPayRate())
                    .payFrequency(comp.getPayFrequency());
        }

        EmployeeEvent event = builder.build();
        kafkaTemplate.send(TOPIC, employee.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} event for employee {}: {}",
                                eventType, employee.getId(), ex.getMessage());
                    } else {
                        log.info("Published {} event for employee {}", eventType, employee.getId());
                    }
                });
    }
}
