package com.propertize.payroll.controller;

import com.propertize.payroll.dto.employee.CreateEmployeeRequest;
import com.propertize.payroll.dto.employee.EmployeeDTO;
import com.propertize.payroll.dto.employee.UpdateEmployeeRequest;
import com.propertize.payroll.enums.EmployeeStatusEnum;
import com.propertize.payroll.service.EmployeeEntityService;
import com.propertize.payroll.service.EmployeeSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

/**
 * Controller for Employee operations.
 * Uses EmployeeEntityService for local CRUD and EmployeeSyncService for Employecraft integration.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping
public class EmployeeController {

    private final EmployeeEntityService employeeService;
    private final EmployeeSyncService employeeSyncService;

    @GetMapping("/clients/{clientId}/employees")
    public ResponseEntity<Page<EmployeeDTO>> getEmployeesByClient(
            @PathVariable UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) EmployeeStatusEnum status,
            @RequestParam(required = false) String department) {
        log.info("Fetching employees for client: {}, page: {}, limit: {}, status: {}",
                clientId, page, limit, status);

        PageRequest pageRequest = PageRequest.of(page, limit);
        Page<EmployeeDTO> employees = employeeService.getEmployeesByClient(clientId, pageRequest);

        return ResponseEntity.ok(employees);
    }

    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeDTO> getEmployeeById(@PathVariable UUID id) {
        log.info("Fetching employee by ID: {}", id);
        return ResponseEntity.ok(employeeService.getEmployeeById(id));
    }

    @PostMapping("/clients/{clientId}/employees")
    public ResponseEntity<EmployeeDTO> createEmployee(
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateEmployeeRequest request) {
        log.info("Creating employee for client: {}", clientId);
        request.setClientId(clientId);
        EmployeeDTO created = employeeService.createEmployee(request);
        return ResponseEntity.created(URI.create("/employees/" + created.getId())).body(created);
    }

    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeDTO> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        log.info("Updating employee: {}", id);
        return ResponseEntity.ok(employeeService.updateEmployee(id, request));
    }

    @PostMapping("/employees/{id}/terminate")
    public ResponseEntity<Void> terminateEmployee(
            @PathVariable UUID id,
            @RequestParam(required = false) java.time.LocalDate terminationDate) {
        log.info("Terminating employee: {} on date: {}", id, terminationDate);
        employeeService.terminateEmployee(id, terminationDate != null ? terminationDate : java.time.LocalDate.now());
        return ResponseEntity.noContent().build();
    }

    /**
     * Sync a single employee from Employecraft
     */
    @PostMapping("/employees/{id}/sync")
    public ResponseEntity<Void> syncEmployee(
            @PathVariable UUID id,
            @RequestParam UUID clientId,
            @RequestHeader("Authorization") String authHeader) {
        log.info("Syncing employee {} from Employecraft", id);
        String token = authHeader.replace("Bearer ", "");
        employeeSyncService.syncEmployee(id, clientId, token);
        return ResponseEntity.ok().build();
    }

    /**
     * Sync all employees for a client from Employecraft
     */
    @PostMapping("/clients/{clientId}/employees/sync")
    public ResponseEntity<Void> syncAllEmployees(
            @PathVariable UUID clientId,
            @RequestParam UUID organizationId,
            @RequestHeader("Authorization") String authHeader) {
        log.info("Syncing all employees for client {} from Employecraft", clientId);
        String token = authHeader.replace("Bearer ", "");
        employeeSyncService.syncAllEmployees(clientId, organizationId, token);
        return ResponseEntity.accepted().build();
    }
}
