package com.propertize.payment.controller;

import com.propertize.payment.config.ApiVersion;
import com.propertize.payment.config.SecurityContext;
import com.propertize.commons.dto.ApiResponse;
import com.propertize.payment.dto.payment.request.*;
import com.propertize.payment.dto.payment.response.PaymentStatisticsResponse;
import com.propertize.payment.entity.Payment;
import com.propertize.payment.service.PaymentService;
import com.propertize.commons.dto.ResponseHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Payment>>> getAllPayments(
            @RequestParam(required = false) String organizationId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Resolve organizationId from request param or JWT-forwarded header
        String resolvedOrgId = (organizationId != null && !organizationId.isBlank())
                ? organizationId
                : SecurityContext.getCurrentOrganizationId();
        if (resolvedOrgId == null || resolvedOrgId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<Payment>>builder()
                            .success(false)
                            .message("organizationId is required")
                            .build());
        }
        return ResponseHandler.handlePaginated(
                () -> paymentService.getAllPayments(resolvedOrgId, tenantId, page, size), "Payments");
    }

    // ──── Specific sub-resource endpoints (MUST come before /{id}) ────────────

    /**
     * GET /api/v1/payments/statistics
     * <p>
     * Returns aggregated payment stats (counts + sums) for the resolved
     * organisation.
     * All filter params are optional.
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<PaymentStatisticsResponse>> getPaymentStatistics(
            @RequestParam(required = false) String organizationId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String leaseId) {
        String resolvedOrgId = resolveOrgId(organizationId);
        if (resolvedOrgId == null) {
            return badOrgId();
        }
        return ResponseHandler.handleFind(
                () -> paymentService.getPaymentStatistics(resolvedOrgId, startDate, endDate, tenantId, leaseId),
                "Payment statistics");
    }

    /**
     * GET /api/v1/payments/due-soon?days=3
     * <p>
     * Returns pending/scheduled payments due within the next {@code days} days.
     */
    @GetMapping("/due-soon")
    public ResponseEntity<ApiResponse<List<Payment>>> getPaymentsDueSoon(
            @RequestParam(required = false) String organizationId,
            @RequestParam(defaultValue = "3") int days) {
        String resolvedOrgId = resolveOrgId(organizationId);
        if (resolvedOrgId == null) {
            return badOrgId();
        }
        return ResponseHandler.handleList(
                () -> paymentService.getPaymentsDueSoon(resolvedOrgId, days),
                "Payments due soon");
    }

    /**
     * GET /api/v1/payments/failed-retry
     * <p>
     * Returns failed payments eligible for retry.
     */
    @GetMapping("/failed-retry")
    public ResponseEntity<ApiResponse<List<Payment>>> getFailedPaymentsForRetry(
            @RequestParam(required = false) String organizationId) {
        String resolvedOrgId = resolveOrgId(organizationId);
        if (resolvedOrgId == null) {
            return badOrgId();
        }
        return ResponseHandler.handleList(
                () -> paymentService.getFailedPaymentsForRetry(resolvedOrgId),
                "Failed payments");
    }

    /**
     * GET /api/v1/payments/recurring-due?date=2026-01-31
     * <p>
     * Returns recurring payments due on or before the given date (defaults to
     * today).
     */
    @GetMapping("/recurring-due")
    public ResponseEntity<ApiResponse<List<Payment>>> getRecurringPaymentsDue(
            @RequestParam(required = false) String organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        String resolvedOrgId = resolveOrgId(organizationId);
        if (resolvedOrgId == null) {
            return badOrgId();
        }
        return ResponseHandler.handleList(
                () -> paymentService.getRecurringPaymentsDue(resolvedOrgId, date),
                "Recurring payments due");
    }

    // ──── By-ID endpoint (MUST come after all specific paths above) ───────────

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Payment>> getPaymentById(@PathVariable String id) {
        return ResponseHandler.handleFind(() -> paymentService.getPaymentById(id), "Payment");
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<ApiResponse<List<Payment>>> getPaymentsByTenant(@PathVariable String tenantId) {
        return ResponseHandler.handleList(() -> paymentService.getPaymentsByTenant(tenantId), "Payments");
    }

    @GetMapping("/lease/{leaseId}")
    public ResponseEntity<ApiResponse<List<Payment>>> getPaymentsByLease(@PathVariable String leaseId) {
        return ResponseHandler.handleList(() -> paymentService.getPaymentsByLease(leaseId), "Payments");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Payment>> createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        return ResponseHandler.handleSave(() -> paymentService.createPayment(request), "Payment");
    }

    @PostMapping("/{id}/process")
    public ResponseEntity<ApiResponse<Payment>> processPayment(
            @PathVariable String id,
            @Valid @RequestBody PaymentProcessRequest request) {
        return ResponseHandler.handleSave(() -> paymentService.processPayment(id, request), "Payment");
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<ApiResponse<Payment>> refundPayment(
            @PathVariable String id,
            @Valid @RequestBody PaymentRefundRequest request) {
        return ResponseHandler.handleSave(() -> paymentService.refundPayment(id, request), "Payment");
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Payment>> updatePayment(
            @PathVariable String id,
            @RequestBody PaymentUpdateRequest request) {
        return ResponseHandler.handleUpdate(() -> paymentService.updatePayment(id, request), "Payment");
    }

    // ──── Private helpers ─────────────────────────────────────────────────────

    private String resolveOrgId(String param) {
        if (param != null && !param.isBlank()) {
            return param;
        }
        return SecurityContext.getCurrentOrganizationId();
    }

    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<ApiResponse<T>> badOrgId() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<T>builder()
                        .success(false)
                        .message("organizationId is required")
                        .build());
    }
}
