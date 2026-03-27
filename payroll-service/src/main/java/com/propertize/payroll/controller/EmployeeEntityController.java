package com.propertize.payroll.controller;

import com.propertize.payroll.dto.common.ApiResponse;
import com.propertize.payroll.dto.employee.CreateEmployeeRequest;
import com.propertize.payroll.dto.employee.EmployeeDTO;
import com.propertize.payroll.dto.employee.UpdateEmployeeRequest;
import com.propertize.payroll.service.EmployeeEntityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Employee Management", description = "APIs for managing employees")
public class EmployeeEntityController {

    private final EmployeeEntityService employeeService;

    @PostMapping
    @Operation(summary = "Create a new employee")
    public ResponseEntity<ApiResponse<EmployeeDTO>> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        log.info("REST request to create employee: {}", request.getEmployeeNumber());
        EmployeeDTO employee = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(employee, "Employee created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID")
    public ResponseEntity<ApiResponse<EmployeeDTO>> getEmployee(@PathVariable UUID id) {
        log.info("REST request to get employee: {}", id);
        EmployeeDTO employee = employeeService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success(employee));
    }

    @GetMapping("/by-number/{employeeNumber}")
    @Operation(summary = "Get employee by employee number")
    public ResponseEntity<ApiResponse<EmployeeDTO>> getEmployeeByNumber(@PathVariable String employeeNumber) {
        log.info("REST request to get employee by number: {}", employeeNumber);
        EmployeeDTO employee = employeeService.getEmployeeByNumber(employeeNumber);
        return ResponseEntity.ok(ApiResponse.success(employee));
    }

    @GetMapping("/client/{clientId}")
    @Operation(summary = "Get all employees for a client")
    public ResponseEntity<ApiResponse<Page<EmployeeDTO>>> getEmployeesByClient(
            @PathVariable UUID clientId,
            Pageable pageable) {
        log.info("REST request to get employees for client: {}", clientId);
        Page<EmployeeDTO> employees = employeeService.getEmployeesByClient(clientId, pageable);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    @GetMapping("/client/{clientId}/active")
    @Operation(summary = "Get all active employees for a client")
    public ResponseEntity<ApiResponse<List<EmployeeDTO>>> getActiveEmployeesByClient(@PathVariable UUID clientId) {
        log.info("REST request to get active employees for client: {}", clientId);
        List<EmployeeDTO> employees = employeeService.getActiveEmployeesByClient(clientId);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    @GetMapping("/client/{clientId}/search")
    @Operation(summary = "Search employees for a client")
    public ResponseEntity<ApiResponse<Page<EmployeeDTO>>> searchEmployees(
            @PathVariable UUID clientId,
            @RequestParam String query,
            Pageable pageable) {
        log.info("REST request to search employees for client: {} with query: {}", clientId, query);
        Page<EmployeeDTO> employees = employeeService.searchEmployees(clientId, query, pageable);
        return ResponseEntity.ok(ApiResponse.success(employees));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an employee")
    public ResponseEntity<ApiResponse<EmployeeDTO>> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        log.info("REST request to update employee: {}", id);
        EmployeeDTO employee = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success(employee, "Employee updated successfully"));
    }

    @PostMapping("/{id}/terminate")
    @Operation(summary = "Terminate an employee")
    public ResponseEntity<ApiResponse<Void>> terminateEmployee(
            @PathVariable UUID id,
            @RequestParam LocalDate terminationDate) {
        log.info("REST request to terminate employee: {} effective {}", id, terminationDate);
        employeeService.terminateEmployee(id, terminationDate);
        return ResponseEntity.ok(ApiResponse.success(null, "Employee terminated successfully"));
    }
}
