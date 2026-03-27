package com.propertize.payment.controller;

import com.propertize.payment.config.ApiVersion;
import com.propertize.payment.dto.common.ApiResponse;
import com.propertize.payment.dto.promo.*;
import com.propertize.payment.entity.PromoCode;
import com.propertize.payment.service.PromoCodeService;
import com.propertize.payment.util.ResponseHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/promo-codes")
@RequiredArgsConstructor
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PromoCode>>> getPromoCodesByOrganization(
            @RequestParam String organizationId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseHandler.handlePaginated(
                () -> promoCodeService.getPromoCodesByOrganization(organizationId, page, size), "PromoCodes");
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromoCode>> getPromoCodeById(@PathVariable String id) {
        return ResponseHandler.handleFind(() -> promoCodeService.getPromoCodeById(id), "PromoCode");
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PromoCode>> createPromoCode(@Valid @RequestBody PromoCodeRequest request) {
        return ResponseHandler.handleSave(() -> promoCodeService.createPromoCode(request), "PromoCode");
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromoCode>> updatePromoCode(
            @PathVariable String id,
            @RequestBody PromoCodeRequest request) {
        return ResponseHandler.handleUpdate(() -> promoCodeService.updatePromoCode(id, request), "PromoCode");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePromoCode(@PathVariable String id) {
        return ResponseHandler.handleDelete(() -> promoCodeService.deletePromoCode(id), "PromoCode");
    }

    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<PromoCodeValidateResponse>> validatePromoCode(
            @Valid @RequestBody PromoCodeValidateRequest request) {
        return ResponseHandler.handleFind(() -> promoCodeService.validatePromoCode(request), "PromoCode");
    }
}
