package com.propertize.payroll.controller;

import com.propertize.payroll.entity.LeaveBalanceEntity;
import com.propertize.payroll.entity.LeaveRequest;
import com.propertize.payroll.service.LeaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/leave")
@RequiredArgsConstructor
@Slf4j
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping("/requests/{id}")
    public ResponseEntity<LeaveRequest> getLeaveRequest(@PathVariable UUID id) {
        return ResponseEntity.ok(leaveService.getLeaveRequest(id));
    }

    @GetMapping("/requests/employee/{employeeId}")
    public ResponseEntity<Page<LeaveRequest>> getEmployeeLeaveRequests(
            @PathVariable String employeeId,
            Pageable pageable) {
        return ResponseEntity.ok(leaveService.getEmployeeLeaveRequests(employeeId, pageable));
    }

    @PostMapping("/requests/{id}/approve")
    public ResponseEntity<LeaveRequest> approveLeaveRequest(
            @PathVariable UUID id,
            @RequestParam UUID approverId) {
        log.info("Approving leave request: {} by: {}", id, approverId);
        return ResponseEntity.ok(leaveService.approveLeaveRequest(id, approverId));
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<LeaveRequest> rejectLeaveRequest(
            @PathVariable UUID id,
            @RequestParam UUID rejectorId,
            @RequestParam String reason) {
        log.info("Rejecting leave request: {} by: {}", id, rejectorId);
        return ResponseEntity.ok(leaveService.rejectLeaveRequest(id, rejectorId, reason));
    }

    @GetMapping("/balances/{employeeId}")
    public ResponseEntity<List<LeaveBalanceEntity>> getLeaveBalances(
            @PathVariable String employeeId,
            @RequestParam Integer year) {
        return ResponseEntity.ok(leaveService.getLeaveBalances(employeeId, year));
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<List<LeaveRequest>> getPendingLeaveRequests() {
        return ResponseEntity.ok(leaveService.getPendingLeaveRequests());
    }
}
