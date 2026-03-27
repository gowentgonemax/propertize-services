package com.propertize.payroll.client;

import com.propertize.payroll.client.dto.EmployeeDto;
import com.propertize.payroll.client.dto.DepartmentDto;
import com.propertize.payroll.client.dto.PositionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Feign Client for Employecraft Service Integration
 *
 * Handles:
 * - Employee data retrieval
 * - Department information
 * - Position information
 *
 * Note: Wagecraft caches employee data locally for payroll calculations
 * but delegates core HR operations to Employecraft
 */
@FeignClient(
    name = "employecraft-service",
    url = "${employecraft.api.url}",
    configuration = FeignClientConfig.class
)
public interface EmployecraftFeignClient {

    // ==================== Employee Operations ====================

    /**
     * Get employee by ID
     */
    @GetMapping("/api/v1/employees/{id}")
    ResponseEntity<EmployeeDto> getEmployee(
        @PathVariable("id") UUID employeeId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Get employee by employee number within organization
     */
    @GetMapping("/api/v1/employees/by-number/{employeeNumber}")
    ResponseEntity<EmployeeDto> getEmployeeByNumber(
        @PathVariable("employeeNumber") String employeeNumber,
        @RequestParam("organizationId") UUID organizationId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * List employees for an organization
     */
    @GetMapping("/api/v1/employees")
    ResponseEntity<Page<EmployeeDto>> listEmployees(
        @RequestParam("organizationId") UUID organizationId,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestParam(value = "status", required = false) String status,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Get employees by IDs (batch fetch)
     */
    @PostMapping("/api/v1/employees/batch")
    ResponseEntity<List<EmployeeDto>> getEmployeesByIds(
        @RequestBody List<UUID> employeeIds,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Search employees
     */
    @GetMapping("/api/v1/employees/search")
    ResponseEntity<Page<EmployeeDto>> searchEmployees(
        @RequestParam("organizationId") UUID organizationId,
        @RequestParam("query") String query,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "20") int size,
        @RequestHeader("Authorization") String authorization
    );

    // ==================== Department Operations ====================

    /**
     * Get department by ID
     */
    @GetMapping("/api/v1/departments/{id}")
    ResponseEntity<DepartmentDto> getDepartment(
        @PathVariable("id") UUID departmentId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * List departments for an organization
     */
    @GetMapping("/api/v1/departments")
    ResponseEntity<List<DepartmentDto>> listDepartments(
        @RequestParam("organizationId") UUID organizationId,
        @RequestHeader("Authorization") String authorization
    );

    // ==================== Position Operations ====================

    /**
     * Get position by ID
     */
    @GetMapping("/api/v1/positions/{id}")
    ResponseEntity<PositionDto> getPosition(
        @PathVariable("id") UUID positionId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * List positions for an organization
     */
    @GetMapping("/api/v1/positions")
    ResponseEntity<List<PositionDto>> listPositions(
        @RequestParam("organizationId") UUID organizationId,
        @RequestHeader("Authorization") String authorization
    );

    // ==================== Validation Operations ====================

    /**
     * Validate employee exists
     */
    @GetMapping("/api/v1/employees/{id}/exists")
    ResponseEntity<Boolean> employeeExists(
        @PathVariable("id") UUID employeeId,
        @RequestHeader("Authorization") String authorization
    );

    /**
     * Get employee count for organization
     */
    @GetMapping("/api/v1/employees/count")
    ResponseEntity<Long> getEmployeeCount(
        @RequestParam("organizationId") UUID organizationId,
        @RequestHeader("Authorization") String authorization
    );
}
