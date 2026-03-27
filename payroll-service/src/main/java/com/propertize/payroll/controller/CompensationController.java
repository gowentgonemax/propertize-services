package com.propertize.payroll.controller;

import com.propertize.payroll.dto.compensation.request.CompensationCreateRequest;
import com.propertize.payroll.dto.compensation.request.CompensationUpdateRequest;
import com.propertize.payroll.dto.compensation.response.CompensationHistoryResponse;
import com.propertize.payroll.dto.compensation.response.CompensationResponse;
import com.propertize.payroll.service.CompensationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing employee compensation.
 *
 * @author WageCraft Team
 * @version 1.0
 * @since 2026-02-05
 */
@RestController
@RequestMapping("/api/v1/compensation")
@RequiredArgsConstructor
@Slf4j
public class CompensationController {

    private final CompensationService compensationService;

    /**
     * Create a new compensation record.
     *
     * POST /api/v1/compensation
     *
     * @param request Compensation creation request
     * @return Created compensation response
     */
    @PostMapping
    @PreAuthorize("hasAuthority('compensation:create')")
    public ResponseEntity<CompensationResponse> createCompensation(
            @Valid @RequestBody CompensationCreateRequest request) {
        log.info("Creating compensation for employee: {}", request.getEmployeeId());

        CompensationResponse response = compensationService.createCompensation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing compensation record.
     *
     * PUT /api/v1/compensation/{id}
     *
     * @param id      Compensation ID
     * @param request Update request
     * @return Updated compensation response
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('compensation:update')")
    public ResponseEntity<CompensationResponse> updateCompensation(
            @PathVariable UUID id,
            @Valid @RequestBody CompensationUpdateRequest request) {
        log.info("Updating compensation: {}", id);

        CompensationResponse response = compensationService.updateCompensation(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get compensation details by ID.
     *
     * GET /api/v1/compensation/{id}
     *
     * @param id Compensation ID
     * @return Compensation response
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('compensation:read')")
    public ResponseEntity<CompensationResponse> getCompensationById(@PathVariable UUID id) {
        log.info("Fetching compensation: {}", id);

        CompensationResponse response = compensationService.getCompensationById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current compensation for an employee.
     *
     * GET /api/v1/compensation/employee/{employeeId}/current
     *
     * @param employeeId Employee ID
     * @return Current compensation response
     */
    @GetMapping("/employee/{employeeId}/current")
    @PreAuthorize("hasAuthority('compensation:read')")
    public ResponseEntity<CompensationResponse> getCurrentCompensation(
            @PathVariable UUID employeeId) {
        log.info("Fetching current compensation for employee: {}", employeeId);

        CompensationResponse response = compensationService.getCurrentCompensation(employeeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get compensation history for an employee.
     *
     * GET /api/v1/compensation/employee/{employeeId}/history
     *
     * @param employeeId Employee ID
     * @return List of compensation history
     */
    @GetMapping("/employee/{employeeId}/history")
    @PreAuthorize("hasAuthority('compensation:read')")
    public ResponseEntity<List<CompensationHistoryResponse>> getCompensationHistory(
            @PathVariable UUID employeeId) {
        log.info("Fetching compensation history for employee: {}", employeeId);

        List<CompensationHistoryResponse> history = compensationService.getCompensationHistory(employeeId);
        return ResponseEntity.ok(history);
    }

    /**
     * Deactivate a compensation record.
     *
     * DELETE /api/v1/compensation/{id}/deactivate
     *
     * @param id      Compensation ID
     * @param endDate End date (optional, defaults to today)
     * @param reason  Deactivation reason
     * @return No content
     */
    @DeleteMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('compensation:delete')")
    public ResponseEntity<Void> deactivateCompensation(
            @PathVariable UUID id,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam String reason) {
        log.info("Deactivating compensation: {}", id);

        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();
        compensationService.deactivateCompensation(id, effectiveEndDate, reason);
        return ResponseEntity.noContent().build();
    }

    /**
     * Delete a compensation record (soft delete).
     *
     * DELETE /api/v1/compensation/{id}
     *
     * @param id Compensation ID
     * @return No content
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('compensation:delete')")
    public ResponseEntity<Void> deleteCompensation(@PathVariable UUID id) {
        log.info("Deleting compensation: {}", id);

        compensationService.deleteCompensation(id);
        return ResponseEntity.noContent().build();
    }
}
