package com.propertize.payment.controller;

import com.propertize.payment.config.ApiVersion;
import com.propertize.commons.dto.ApiResponse;
import com.propertize.payment.dto.payment.request.ApplicationFeeProcessPaymentRequest;
import com.propertize.payment.dto.payment.request.ApplicationFeeRequest;
import com.propertize.payment.entity.ApplicationFee;
import com.propertize.payment.service.ApplicationFeeService;
import com.propertize.commons.dto.ResponseHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/application-fees")
@RequiredArgsConstructor
public class ApplicationFeeController {

    private final ApplicationFeeService applicationFeeService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApplicationFee>> getApplicationFeeById(@PathVariable String id) {
        return ResponseHandler.handleFind(() -> applicationFeeService.getApplicationFeeById(id), "ApplicationFee");
    }

    @GetMapping("/by-application/{rentalApplicationId}")
    public ResponseEntity<ApiResponse<ApplicationFee>> getApplicationFeeByRentalApplication(
            @PathVariable String rentalApplicationId) {
        return ResponseHandler.handleFind(
                () -> applicationFeeService.getApplicationFeeByRentalApplicationId(rentalApplicationId),
                "ApplicationFee");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ApplicationFee>> createApplicationFee(
            @Valid @RequestBody ApplicationFeeRequest request) {
        return ResponseHandler.handleSave(() -> applicationFeeService.createApplicationFee(request), "ApplicationFee");
    }

    @PostMapping("/{id}/process-payment")
    public ResponseEntity<ApiResponse<ApplicationFee>> processPayment(
            @PathVariable String id,
            @RequestBody ApplicationFeeProcessPaymentRequest request) {
        return ResponseHandler.handleSave(() -> applicationFeeService.processPayment(id, request), "ApplicationFee");
    }
}
