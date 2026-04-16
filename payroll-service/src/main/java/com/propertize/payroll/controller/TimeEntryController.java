package com.propertize.payroll.controller;

import com.propertize.commons.dto.ApiResponse;
import com.propertize.payroll.dto.timeentry.CreateTimeEntryRequest;
import com.propertize.payroll.dto.timeentry.TimeEntryDTO;
import com.propertize.payroll.service.TimeEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/time-entries")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Time Entry Management", description = "APIs for managing time entries")
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    @PostMapping
    @Operation(summary = "Create a new time entry")
    public ResponseEntity<ApiResponse<TimeEntryDTO>> createTimeEntry(@Valid @RequestBody CreateTimeEntryRequest request) {
        log.info("REST request to create time entry for employee: {}", request.getEmployeeId());
        TimeEntryDTO timeEntry = timeEntryService.createTimeEntry(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(timeEntry, "Time entry created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get time entry by ID")
    public ResponseEntity<ApiResponse<TimeEntryDTO>> getTimeEntry(@PathVariable UUID id) {
        log.info("REST request to get time entry: {}", id);
        TimeEntryDTO timeEntry = timeEntryService.getTimeEntryById(id);
        return ResponseEntity.ok(ApiResponse.success(timeEntry));
    }

    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get time entries for an employee")
    public ResponseEntity<ApiResponse<Page<TimeEntryDTO>>> getTimeEntriesByEmployee(
            @PathVariable UUID employeeId,
            Pageable pageable) {
        log.info("REST request to get time entries for employee: {}", employeeId);
        Page<TimeEntryDTO> timeEntries = timeEntryService.getTimeEntriesByEmployee(employeeId, pageable);
        return ResponseEntity.ok(ApiResponse.success(timeEntries));
    }

    @GetMapping("/employee/{employeeId}/range")
    @Operation(summary = "Get time entries for an employee within a date range")
    public ResponseEntity<ApiResponse<List<TimeEntryDTO>>> getTimeEntriesByEmployeeAndDateRange(
            @PathVariable UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to get time entries for employee: {} from {} to {}", employeeId, startDate, endDate);
        List<TimeEntryDTO> timeEntries = timeEntryService.getTimeEntriesByEmployeeAndDateRange(employeeId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(timeEntries));
    }

    @GetMapping("/client/{clientId}/range")
    @Operation(summary = "Get time entries for a client within a date range")
    public ResponseEntity<ApiResponse<List<TimeEntryDTO>>> getTimeEntriesByClientAndDateRange(
            @PathVariable UUID clientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to get time entries for client: {} from {} to {}", clientId, startDate, endDate);
        List<TimeEntryDTO> timeEntries = timeEntryService.getTimeEntriesByClientAndDateRange(clientId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(timeEntries));
    }

    @GetMapping("/client/{clientId}/pending")
    @Operation(summary = "Get pending time entries for a client")
    public ResponseEntity<ApiResponse<Page<TimeEntryDTO>>> getPendingTimeEntriesByClient(
            @PathVariable UUID clientId,
            Pageable pageable) {
        log.info("REST request to get pending time entries for client: {}", clientId);
        Page<TimeEntryDTO> timeEntries = timeEntryService.getPendingTimeEntriesByClient(clientId, pageable);
        return ResponseEntity.ok(ApiResponse.success(timeEntries));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a time entry")
    public ResponseEntity<ApiResponse<TimeEntryDTO>> approveTimeEntry(
            @PathVariable UUID id,
            @RequestParam UUID approverId) {
        log.info("REST request to approve time entry: {} by user: {}", id, approverId);
        TimeEntryDTO timeEntry = timeEntryService.approveTimeEntry(id, approverId);
        return ResponseEntity.ok(ApiResponse.success(timeEntry, "Time entry approved successfully"));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a time entry")
    public ResponseEntity<ApiResponse<TimeEntryDTO>> rejectTimeEntry(
            @PathVariable UUID id,
            @RequestParam(required = false) String rejectionNotes) {
        log.info("REST request to reject time entry: {}", id);
        TimeEntryDTO timeEntry = timeEntryService.rejectTimeEntry(id, rejectionNotes);
        return ResponseEntity.ok(ApiResponse.success(timeEntry, "Time entry rejected"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a time entry")
    public ResponseEntity<ApiResponse<Void>> deleteTimeEntry(@PathVariable UUID id) {
        log.info("REST request to delete time entry: {}", id);
        timeEntryService.deleteTimeEntry(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Time entry deleted successfully"));
    }

    @GetMapping("/employee/{employeeId}/hours")
    @Operation(summary = "Get total approved hours for an employee")
    public ResponseEntity<ApiResponse<Object>> getTotalApprovedHours(
            @PathVariable UUID employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("REST request to get total approved hours for employee: {}", employeeId);
        BigDecimal regularHours = timeEntryService.getTotalApprovedRegularHours(employeeId, startDate, endDate);
        BigDecimal overtimeHours = timeEntryService.getTotalApprovedOvertimeHours(employeeId, startDate, endDate);

        var response = new java.util.HashMap<String, BigDecimal>();
        response.put("regularHours", regularHours);
        response.put("overtimeHours", overtimeHours);
        response.put("totalHours", regularHours.add(overtimeHours));

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
