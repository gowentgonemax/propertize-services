package com.propertize.platform.employecraft.controller;

import com.propertize.platform.employecraft.dto.DepartmentSummary;
import com.propertize.platform.employecraft.dto.PositionSummary;
import com.propertize.platform.employecraft.dto.employee.request.EmployeeCreateRequest;
import com.propertize.platform.employecraft.dto.employee.response.EmployeePayrollSummary;
import com.propertize.platform.employecraft.dto.employee.response.EmployeeResponse;
import com.propertize.platform.employecraft.service.DepartmentService;
import com.propertize.platform.employecraft.service.EmployeeService;
import com.propertize.platform.employecraft.service.PositionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Employee Management Controller
 */
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final DepartmentService departmentService;
    private final PositionService positionService;

    @GetMapping
    public ResponseEntity<Page<EmployeeResponse>> getAllEmployees(
            @RequestParam(required = false) UUID organizationId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(employeeService.getAllEmployees(organizationId, pageable));
    }

    /**
     * Returns all active departments for the current organization.
     * Used to populate department dropdowns in the employee creation/edit UI.
     */
    @GetMapping("/departments")
    public ResponseEntity<List<DepartmentSummary>> getDepartments() {
        return ResponseEntity.ok(departmentService.getActiveDepartments());
    }

    /**
     * Returns all active positions for the current organization.
     * Used to populate position dropdowns in the employee creation/edit UI.
     */
    @GetMapping("/positions")
    public ResponseEntity<List<PositionSummary>> getPositions() {
        return ResponseEntity.ok(positionService.getActivePositions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable UUID id) {
        return ResponseEntity.ok(employeeService.getEmployee(id));
    }

    @GetMapping("/by-user/{userId}")
    public ResponseEntity<EmployeeResponse> getEmployeeByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(employeeService.getEmployeeByUserId(userId));
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeResponse> getMyEmployeeProfile() {
        return employeeService.getMyEmployeeProfile()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request,
            @RequestParam(required = false) UUID organizationId) {
        EmployeeResponse response = employeeService.createEmployee(request, organizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<EmployeeResponse> activateEmployee(@PathVariable UUID id) {
        return ResponseEntity.ok(employeeService.activateEmployee(id));
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<EmployeeResponse> terminateEmployee(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(employeeService.terminateEmployee(id, reason));
    }

    /**
     * Incremental sync endpoint — returns employees modified after the given
     * timestamp.
     * Used by payroll-service to fetch only changed records.
     */
    @GetMapping("/changed-since")
    public ResponseEntity<Page<EmployeeResponse>> getChangedSince(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
            @PageableDefault(size = 100) Pageable pageable) {
        return ResponseEntity.ok(employeeService.getChangedSince(since, pageable));
    }

    /**
     * Payroll-ready summary — returns minimal employee data for payroll processing.
     * Only includes ACTIVE and ON_LEAVE employees.
     */
    @GetMapping("/payroll-summary")
    public ResponseEntity<List<EmployeePayrollSummary>> getPayrollSummaries() {
        return ResponseEntity.ok(employeeService.getPayrollSummaries());
    }
}
