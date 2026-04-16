package com.propertize.payment.controller;

import com.propertize.payment.config.ApiVersion;
import com.propertize.commons.dto.ApiResponse;
import com.propertize.payment.entity.OrganizationApplicationFee;
import com.propertize.payment.service.OrganizationApplicationFeeService;
import com.propertize.commons.dto.ResponseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping(ApiVersion.V1 + "/organization-application-fees")
@RequiredArgsConstructor
public class OrganizationApplicationFeeController {

    private final OrganizationApplicationFeeService orgApplicationFeeService;

    @GetMapping("/tracking/{trackingId}")
    public ResponseEntity<ApiResponse<OrganizationApplicationFee>> getByTrackingId(@PathVariable String trackingId) {
        return ResponseHandler.handleFind(() -> orgApplicationFeeService.getByTrackingId(trackingId),
                "OrganizationApplicationFee");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationApplicationFee>> getById(@PathVariable String id) {
        return ResponseHandler.handleFind(() -> orgApplicationFeeService.getById(id), "OrganizationApplicationFee");
    }

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<OrganizationApplicationFee>> initiate(
            @RequestParam String organizationName,
            @RequestParam String organizationEmail,
            @RequestParam(defaultValue = "0.00") BigDecimal feeAmount) {
        return ResponseHandler.handleSave(
                () -> orgApplicationFeeService.initiate(organizationName, organizationEmail, feeAmount),
                "OrganizationApplicationFee");
    }

    @PostMapping("/{trackingId}/complete")
    public ResponseEntity<ApiResponse<OrganizationApplicationFee>> completePayment(
            @PathVariable String trackingId,
            @RequestParam(required = false) String stripePaymentMethodId) {
        return ResponseHandler.handleSave(
                () -> orgApplicationFeeService.completePayment(trackingId, stripePaymentMethodId),
                "OrganizationApplicationFee");
    }

    @GetMapping("/tracking/{trackingId}/status")
    public ResponseEntity<ApiResponse<Boolean>> isFeePaid(@PathVariable String trackingId) {
        return ResponseHandler.handleFind(() -> orgApplicationFeeService.isFeePaid(trackingId), "FeePaidStatus");
    }
}
