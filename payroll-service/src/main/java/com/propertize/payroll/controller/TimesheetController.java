package com.propertize.payroll.controller;

import com.propertize.payroll.dto.timesheet.response.TimesheetResponse;
import com.propertize.payroll.service.TimesheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/timesheets")
@RequiredArgsConstructor
@Slf4j
public class TimesheetController {

    private final TimesheetService timesheetService;

    @GetMapping("/{id}")
    public ResponseEntity<TimesheetResponse> getTimesheet(@PathVariable UUID id) {
        return ResponseEntity.ok(timesheetService.getTimesheet(id));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<Page<TimesheetResponse>> getEmployeeTimesheets(
            @PathVariable String employeeId,
            Pageable pageable) {
        return ResponseEntity.ok(timesheetService.getEmployeeTimesheets(employeeId, pageable));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<TimesheetResponse> submitTimesheet(@PathVariable UUID id) {
        log.info("Submitting timesheet: {}", id);
        return ResponseEntity.ok(timesheetService.submitTimesheet(id));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<TimesheetResponse> approveTimesheet(
            @PathVariable UUID id,
            @RequestParam UUID approverId) {
        log.info("Approving timesheet: {} by: {}", id, approverId);
        return ResponseEntity.ok(timesheetService.approveTimesheet(id, approverId));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<TimesheetResponse> rejectTimesheet(
            @PathVariable UUID id,
            @RequestParam UUID rejectorId,
            @RequestParam String reason) {
        log.info("Rejecting timesheet: {} by: {}", id, rejectorId);
        return ResponseEntity.ok(timesheetService.rejectTimesheet(id, rejectorId, reason));
    }

    @GetMapping("/pending/{clientId}")
    public ResponseEntity<List<TimesheetResponse>> getPendingApprovals(
            @PathVariable UUID clientId) {
        return ResponseEntity.ok(timesheetService.getPendingApprovals(clientId));
    }
}
