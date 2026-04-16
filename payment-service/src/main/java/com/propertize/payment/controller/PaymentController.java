package com.propertize.payment.controller;

import com.propertize.payment.config.ApiVersion;
import com.propertize.commons.dto.ApiResponse;
import com.propertize.payment.dto.payment.request.*;
import com.propertize.payment.entity.Payment;
import com.propertize.payment.service.PaymentService;
import com.propertize.commons.dto.ResponseHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Payment>>> getAllPayments(
            @RequestParam String organizationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseHandler.handlePaginated(
                () -> paymentService.getAllPayments(organizationId, page, size), "Payments");
    }

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
}
