package com.propertize.payment.controller;

import com.propertize.payment.config.ApiVersion;
import com.propertize.payment.dto.common.ApiResponse;
import com.propertize.payment.dto.payment.request.CreateACHPaymentMethodRequest;
import com.propertize.payment.dto.payment.request.CreateStripePaymentMethodRequest;
import com.propertize.payment.entity.PaymentMethod;
import com.propertize.payment.service.PaymentMethodService;
import com.propertize.payment.util.ResponseHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<ApiResponse<List<PaymentMethod>>> getPaymentMethodsByTenant(@PathVariable String tenantId) {
        return ResponseHandler.handleList(() -> paymentMethodService.getPaymentMethodsByTenant(tenantId),
                "PaymentMethods");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentMethod>> getPaymentMethodById(@PathVariable String id) {
        return ResponseHandler.handleFind(() -> paymentMethodService.getPaymentMethodById(id), "PaymentMethod");
    }

    @PostMapping("/card")
    public ResponseEntity<ApiResponse<PaymentMethod>> addCardPaymentMethod(
            @Valid @RequestBody CreateStripePaymentMethodRequest request) {
        return ResponseHandler.handleSave(() -> paymentMethodService.addCardPaymentMethod(request), "PaymentMethod");
    }

    @PostMapping("/ach")
    public ResponseEntity<ApiResponse<PaymentMethod>> addACHPaymentMethod(
            @Valid @RequestBody CreateACHPaymentMethodRequest request) {
        return ResponseHandler.handleSave(() -> paymentMethodService.addACHPaymentMethod(request), "PaymentMethod");
    }

    @PostMapping("/{id}/default")
    public ResponseEntity<ApiResponse<Void>> setDefaultPaymentMethod(
            @PathVariable String id,
            @RequestParam String tenantId) {
        return ResponseHandler.handleDelete(
                () -> paymentMethodService.setDefaultPaymentMethod(id, tenantId), "PaymentMethod");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePaymentMethod(@PathVariable String id) {
        return ResponseHandler.handleDelete(() -> paymentMethodService.deletePaymentMethod(id), "PaymentMethod");
    }
}
