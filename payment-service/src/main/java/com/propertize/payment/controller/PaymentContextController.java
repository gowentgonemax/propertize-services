package com.propertize.payment.controller;

import com.propertize.payment.config.ApiVersion;
import com.propertize.payment.dto.common.ApiResponse;
import com.propertize.payment.dto.payment.request.*;
import com.propertize.payment.entity.Payment;
import com.propertize.payment.service.PaymentContextService;
import com.propertize.payment.util.ResponseHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/payments")
@RequiredArgsConstructor
public class PaymentContextController {

    private final PaymentContextService paymentContextService;

    @PostMapping("/vendor")
    public ResponseEntity<ApiResponse<Payment>> createVendorPayment(
            @Valid @RequestBody VendorPaymentRequest request) {
        return ResponseHandler.handleSave(() -> paymentContextService.createVendorPayment(request), "Payment");
    }

    @PostMapping("/platform-subscription")
    public ResponseEntity<ApiResponse<Payment>> createPlatformSubscriptionPayment(
            @Valid @RequestBody PlatformSubscriptionPaymentRequest request) {
        return ResponseHandler.handleSave(() -> paymentContextService.createPlatformSubscriptionPayment(request),
                "Payment");
    }

    @PostMapping("/owner-payout")
    public ResponseEntity<ApiResponse<Payment>> createOwnerPayout(
            @Valid @RequestBody OwnerPayoutRequest request) {
        return ResponseHandler.handleSave(() -> paymentContextService.createOwnerPayout(request), "Payment");
    }
}
