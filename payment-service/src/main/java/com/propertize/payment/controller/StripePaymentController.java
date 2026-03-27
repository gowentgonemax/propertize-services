package com.propertize.payment.controller;

import com.propertize.payment.config.ApiVersion;
import com.propertize.payment.dto.common.ApiResponse;
import com.propertize.payment.dto.payment.request.*;
import com.propertize.payment.dto.payment.response.*;
import com.propertize.payment.service.payment.StripePaymentService;
import com.propertize.payment.util.ResponseHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/stripe")
@RequiredArgsConstructor
public class StripePaymentController {

    private final StripePaymentService stripePaymentService;

    @PostMapping("/payment-intents")
    public ResponseEntity<ApiResponse<StripePaymentIntentResponse>> createPaymentIntent(
            @Valid @RequestBody StripePaymentIntentRequest request) {
        return ResponseHandler.handleSave(() -> stripePaymentService.createPaymentIntent(request), "PaymentIntent");
    }

    @PostMapping("/payment-intents/{id}/confirm")
    public ResponseEntity<ApiResponse<StripePaymentIntentResponse>> confirmPaymentIntent(
            @PathVariable String id,
            @RequestParam(required = false) String paymentMethodId) {
        return ResponseHandler.handleSave(() -> stripePaymentService.confirmPaymentIntent(id, paymentMethodId),
                "PaymentIntent");
    }

    @PostMapping("/payment-intents/{id}/capture")
    public ResponseEntity<ApiResponse<StripePaymentIntentResponse>> capturePaymentIntent(@PathVariable String id) {
        return ResponseHandler.handleSave(() -> stripePaymentService.capturePaymentIntent(id), "PaymentIntent");
    }

    @PostMapping("/payment-intents/{id}/cancel")
    public ResponseEntity<ApiResponse<StripePaymentIntentResponse>> cancelPaymentIntent(@PathVariable String id) {
        return ResponseHandler.handleSave(() -> stripePaymentService.cancelPaymentIntent(id), "PaymentIntent");
    }

    @PostMapping("/refunds")
    public ResponseEntity<ApiResponse<StripeRefundResponse>> createRefund(
            @Valid @RequestBody StripeRefundRequest request) {
        return ResponseHandler.handleSave(() -> stripePaymentService.createRefund(request), "Refund");
    }

    @GetMapping("/payment-intents/{id}")
    public ResponseEntity<ApiResponse<StripePaymentIntentResponse>> retrievePaymentIntent(@PathVariable String id) {
        return ResponseHandler.handleFind(() -> stripePaymentService.retrievePaymentIntent(id), "PaymentIntent");
    }
}
