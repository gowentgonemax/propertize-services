package com.propertize.payment.controller;

import com.propertize.payment.config.ApiVersion;
import com.propertize.commons.dto.ApiResponse;
import com.propertize.payment.dto.payment.request.StripeCustomerRequest;
import com.propertize.payment.dto.payment.response.StripeCustomerResponse;
import com.propertize.payment.service.payment.StripePaymentService;
import com.propertize.commons.dto.ResponseHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/stripe/customers")
@RequiredArgsConstructor
public class StripeCustomerController {

    private final StripePaymentService stripePaymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<StripeCustomerResponse>> createCustomer(
            @Valid @RequestBody StripeCustomerRequest request) {
        return ResponseHandler.handleSave(() -> stripePaymentService.createCustomer(request), "StripeCustomer");
    }

    @PostMapping("/{customerId}/payment-methods/{paymentMethodId}/attach")
    public ResponseEntity<ApiResponse<Void>> attachPaymentMethod(
            @PathVariable String customerId,
            @PathVariable String paymentMethodId) {
        return ResponseHandler.handleDelete(
                () -> stripePaymentService.attachPaymentMethod(paymentMethodId, customerId), "PaymentMethod");
    }

    @DeleteMapping("/payment-methods/{paymentMethodId}/detach")
    public ResponseEntity<ApiResponse<Void>> detachPaymentMethod(@PathVariable String paymentMethodId) {
        return ResponseHandler.handleDelete(
                () -> stripePaymentService.detachPaymentMethod(paymentMethodId), "PaymentMethod");
    }
}
