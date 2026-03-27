package com.propertize.payroll.controller;

import com.propertize.payroll.entity.PayrollRun;
import com.propertize.payroll.enums.PayrollStatusEnum;
import com.propertize.payroll.security.TrustedGatewayHeaderFilter;
import com.propertize.payroll.service.PayrollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clients/{clientId}/payroll")
@RequiredArgsConstructor
public class PayrollController {
    private final PayrollService payrollService;

    @GetMapping
    public ResponseEntity<Page<PayrollRun>> getPayrollRuns(
            @PathVariable UUID clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) PayrollStatusEnum status) {

        if (startDate != null && endDate != null) {
            List<PayrollRun> payrollRuns = payrollService.getPayrollRunsByDateRange(clientId, startDate, endDate);
            Page<PayrollRun> pagedResult = new PageImpl<>(payrollRuns);
            return ResponseEntity.ok().body(pagedResult);
        }

        return ResponseEntity.ok(payrollService.getPayrollRunsByClient(clientId, PageRequest.of(page, limit)));
    }

    @GetMapping("/{payrollId}")
    public ResponseEntity<PayrollRun> getPayrollRun(
            @PathVariable UUID clientId,
            @PathVariable UUID payrollId) {
        return ResponseEntity.ok(payrollService.getPayrollRunById(payrollId));
    }

    @PostMapping
    public ResponseEntity<PayrollRun> createPayrollRun(
            @PathVariable UUID clientId,
            @Valid @RequestBody PayrollRun payrollRun) {
        PayrollRun created = payrollService.createPayrollRun(payrollRun);
        return ResponseEntity.created(URI.create("/payroll/" + created.getId())).body(created);
    }

    @PostMapping("/{payrollId}/process")
    public ResponseEntity<PayrollRun> processPayrollRun(
            @PathVariable UUID clientId,
            @PathVariable UUID payrollId) {
        return ResponseEntity.ok(payrollService.processPayrollRun(payrollId));
    }

    @PostMapping("/{payrollId}/approve")
    public ResponseEntity<PayrollRun> approvePayrollRun(
            @PathVariable UUID clientId,
            @PathVariable UUID payrollId,
            @AuthenticationPrincipal Object principal) {
        String approverName = (principal instanceof TrustedGatewayHeaderFilter.GatewayAuthenticatedUser gau)
                ? gau.getUsername()
                : "system";
        return ResponseEntity.ok(payrollService.approvePayrollRun(payrollId, approverName));
    }
}
