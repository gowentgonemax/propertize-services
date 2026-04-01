package com.propertize.payroll.service;

import com.propertize.payroll.client.EmployecraftFeignClient;
import com.propertize.payroll.client.dto.EmployeeDto;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for synchronizing employee data from Employecraft microservice.
 *
 * Wagecraft maintains a local cache of employee data for:
 * - Payroll calculations (need SSN, pay rates, tax info)
 * - Offline processing
 * - Performance optimization
 *
 * Core HR operations (create, update, terminate) are delegated to Employecraft.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeSyncService {

    private final EmployecraftFeignClient employecraftClient;
    private final EmployeeEntityRepository employeeRepository;
    private final ClientRepository clientRepository;

    /**
     * Sync a single employee from Employecraft
     */
    @Transactional
    @CacheEvict(value = "employees", key = "#employeeId")
    public EmployeeEntity syncEmployee(UUID employeeId, UUID clientId, String authToken) {
        log.info("Syncing employee {} for client {}", employeeId, clientId);

        try {
            ResponseEntity<EmployeeDto> response = employecraftClient.getEmployee(
                    employeeId,
                    "Bearer " + authToken);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return updateLocalEmployee(response.getBody(), clientId);
            } else {
                log.warn("Failed to fetch employee {} from Employecraft", employeeId);
                return null;
            }
        } catch (Exception e) {
            log.error("Error syncing employee {}: {}", employeeId, e.getMessage());
            throw new RuntimeException("Failed to sync employee from Employecraft", e);
        }
    }

    /**
     * Sync all employees for a client
     */
    @Async
    @Transactional
    public CompletableFuture<Integer> syncAllEmployees(UUID clientId, UUID organizationId, String authToken) {
        log.info("Starting full employee sync for client {} (organization {})", clientId, organizationId);

        int syncedCount = 0;
        int page = 0;
        int pageSize = 100;
        boolean hasMore = true;

        try {
            while (hasMore) {
                var response = employecraftClient.listEmployees(
                        organizationId,
                        page,
                        pageSize,
                        null,
                        "Bearer " + authToken);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    var employeePage = response.getBody();

                    for (EmployeeDto dto : employeePage.getContent()) {
                        updateLocalEmployee(dto, clientId);
                        syncedCount++;
                    }

                    hasMore = !employeePage.isLast();
                    page++;
                } else {
                    hasMore = false;
                }
            }

            log.info("Completed employee sync for client {}. Synced {} employees", clientId, syncedCount);
        } catch (Exception e) {
            log.error("Error during full employee sync for client {}: {}", clientId, e.getMessage());
        }

        return CompletableFuture.completedFuture(syncedCount);
    }

    /**
     * Update or create local employee record
     */
    @Transactional
    protected EmployeeEntity updateLocalEmployee(EmployeeDto dto, UUID clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        // Find existing or create new
        EmployeeEntity employee = employeeRepository
                .findByExternalEmployeeId(dto.getId())
                .orElse(new EmployeeEntity());

        // Map from DTO to Entity
        employee.setClient(client);
        employee.setExternalEmployeeId(dto.getId());
        employee.setEmployeeNumber(dto.getEmployeeNumber());
        employee.setFirstName(dto.getFirstName());
        employee.setMiddleName(dto.getMiddleName());
        employee.setLastName(dto.getLastName());
        employee.setDateOfBirth(dto.getDateOfBirth());
        employee.setSsnEncrypted(dto.getSsnEncrypted());
        employee.setHireDate(dto.getHireDate());
        employee.setTerminationDate(dto.getTerminationDate());
        employee.setJobTitle(dto.getJobTitle());

        // Map status
        if (dto.getStatus() != null) {
            try {
                employee.setStatus(EmployeeStatusEnum.valueOf(dto.getStatus()));
            } catch (IllegalArgumentException e) {
                employee.setStatus(EmployeeStatusEnum.ACTIVE);
            }
        }

        // Map employment type
        if (dto.getEmploymentType() != null) {
            try {
                employee.setEmploymentType(EmploymentTypeEnum.valueOf(dto.getEmploymentType()));
            } catch (IllegalArgumentException e) {
                employee.setEmploymentType(EmploymentTypeEnum.FULL_TIME);
            }
        }

        // Map compensation
        if (dto.getCompensation() != null) {
            var comp = dto.getCompensation();
            if (comp.getPayType() != null) {
                try {
                    employee.setPayType(PayTypeEnum.valueOf(comp.getPayType()));
                } catch (IllegalArgumentException e) {
                    employee.setPayType(PayTypeEnum.HOURLY);
                }
            }
            if (comp.getPayFrequency() != null) {
                try {
                    employee.setPayFrequency(PayFrequencyEnum.valueOf(comp.getPayFrequency()));
                } catch (IllegalArgumentException e) {
                    employee.setPayFrequency(PayFrequencyEnum.BI_WEEKLY);
                }
            }
            employee.setHourlyRate(comp.getHourlyRate());
            employee.setAnnualSalary(comp.getAnnualSalary());
        }

        // Map department
        if (dto.getDepartmentId() != null) {
            employee.setDepartmentId(dto.getDepartmentId());
            employee.setDepartmentName(dto.getDepartmentName());
        }

        return employeeRepository.save(employee);
    }

    /**
     * Get employee with fallback to Employecraft if not in cache
     */
    @Cacheable(value = "employees", key = "#externalEmployeeId")
    public Optional<EmployeeEntity> getEmployeeByExternalId(UUID externalEmployeeId) {
        return employeeRepository.findByExternalEmployeeId(externalEmployeeId);
    }

    /**
     * Scheduled sync - runs every 6 hours
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void scheduledSync() {
        log.info("Starting scheduled employee sync");

        // Get all active clients
        List<Client> clients = clientRepository.findAll();

        for (Client client : clients) {
            if (client.getOrganizationId() != null && client.getServiceAccountToken() != null) {
                syncAllEmployees(
                        client.getId(),
                        client.getOrganizationId(),
                        client.getServiceAccountToken());
            }
        }
    }

    /**
     * Validate employee exists in Employecraft
     */
    public boolean validateEmployee(UUID employeeId, String authToken) {
        try {
            ResponseEntity<Boolean> response = employecraftClient.employeeExists(
                    employeeId,
                    "Bearer " + authToken);
            return Boolean.TRUE.equals(response.getBody());
        } catch (Exception e) {
            log.error("Error validating employee {}: {}", employeeId, e.getMessage());
            return false;
        }
    }
}
