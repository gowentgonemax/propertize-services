package com.propertize.payment.service;

import com.propertize.payment.dto.promo.*;
import com.propertize.payment.entity.PromoCode;
import com.propertize.payment.entity.PromoCodeUsage;
import com.propertize.payment.enums.DiscountTypeEnum;
import com.propertize.commons.exception.BadRequestException;
import com.propertize.commons.exception.ResourceNotFoundException;
import com.propertize.payment.repository.PromoCodeRepository;
import com.propertize.payment.repository.PromoCodeUsageRepository;
import com.propertize.payment.util.PaginationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository promoCodeUsageRepository;

    // ──────────────────────── CRUD ────────────────────────

    public Page<PromoCode> getPromoCodesByOrganization(String organizationId, int page, int size) {
        Pageable pageable = PaginationValidator.createPageable(page, size, "createdAt", "desc");
        return promoCodeRepository.findByOrganizationId(organizationId, pageable);
    }

    public PromoCode getPromoCodeById(String id) {
        return promoCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PromoCode", "id", id));
    }

    @Transactional
    public PromoCode createPromoCode(PromoCodeRequest request) {
        // Check uniqueness within org
        promoCodeRepository.findByCodeIgnoreCaseAndOrganizationId(request.getCode(), request.getOrganizationId())
                .ifPresent(existing -> {
                    throw new BadRequestException(
                            "Promo code '" + request.getCode() + "' already exists for this organization");
                });

        validateDiscountValue(request.getDiscountType(), request.getDiscountValue());

        PromoCode promoCode = new PromoCode();
        promoCode.setCode(request.getCode().toUpperCase());
        promoCode.setDescription(request.getDescription());
        promoCode.setOrganizationId(request.getOrganizationId());
        promoCode.setDiscountType(request.getDiscountType());
        promoCode.setDiscountValue(request.getDiscountValue());
        promoCode.setMaxUses(request.getMaxUses());
        promoCode.setCurrentUses(0);
        promoCode.setExpiresAt(request.getExpiresAt());
        promoCode.setActive(request.isActive());
        return promoCodeRepository.save(promoCode);
    }

    @Transactional
    public PromoCode updatePromoCode(String id, PromoCodeRequest request) {
        PromoCode promoCode = getPromoCodeById(id);
        if (request.getDescription() != null)
            promoCode.setDescription(request.getDescription());
        if (request.getDiscountType() != null)
            promoCode.setDiscountType(request.getDiscountType());
        if (request.getDiscountValue() != null) {
            validateDiscountValue(
                    request.getDiscountType() != null ? request.getDiscountType() : promoCode.getDiscountType(),
                    request.getDiscountValue());
            promoCode.setDiscountValue(request.getDiscountValue());
        }
        if (request.getMaxUses() != null)
            promoCode.setMaxUses(request.getMaxUses());
        if (request.getExpiresAt() != null)
            promoCode.setExpiresAt(request.getExpiresAt());
        promoCode.setActive(request.isActive());
        return promoCodeRepository.save(promoCode);
    }

    @Transactional
    public void deletePromoCode(String id) {
        PromoCode promoCode = getPromoCodeById(id);
        promoCode.setActive(false);
        promoCodeRepository.save(promoCode);
    }

    // ──────────────────────── Validate ────────────────────────

    public PromoCodeValidateResponse validatePromoCode(PromoCodeValidateRequest request) {
        PromoCodeValidateResponse response = new PromoCodeValidateResponse();
        response.setCode(request.getCode().toUpperCase());

        PromoCode promoCode = promoCodeRepository
                .findByCodeIgnoreCaseAndOrganizationId(request.getCode(), request.getOrganizationId())
                .orElse(null);

        if (promoCode == null) {
            response.setValid(false);
            response.setMessage("Promo code not found");
            return response;
        }

        if (!Boolean.TRUE.equals(promoCode.getActive())) {
            response.setValid(false);
            response.setMessage("Promo code is not active");
            return response;
        }

        if (promoCode.getExpiresAt() != null && promoCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            response.setValid(false);
            response.setMessage("Promo code has expired");
            return response;
        }

        if (promoCode.getMaxUses() != null && promoCode.getCurrentUses() >= promoCode.getMaxUses()) {
            response.setValid(false);
            response.setMessage("Promo code has reached its maximum usage limit");
            return response;
        }

        // Check if already used for this application
        if (request.getApplicationId() != null &&
                promoCodeUsageRepository.existsByPromoCodeIdAndApplicationId(
                        promoCode.getId(), request.getApplicationId())) {
            response.setValid(false);
            response.setMessage("Promo code has already been applied to this application");
            return response;
        }

        response.setValid(true);
        response.setPromoCodeId(promoCode.getId());
        response.setDiscountType(promoCode.getDiscountType());
        response.setDiscountValue(promoCode.getDiscountValue());
        response.setMessage("Promo code is valid");
        return response;
    }

    /**
     * Calculate discount amount based on promo code rules.
     */
    public BigDecimal calculateDiscount(String promoCodeId, BigDecimal baseAmount) {
        PromoCode promoCode = getPromoCodeById(promoCodeId);

        return switch (promoCode.getDiscountType()) {
            case PERCENTAGE -> baseAmount
                    .multiply(promoCode.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FIXED -> promoCode.getDiscountValue().min(baseAmount); // can't exceed base
            case WAIVE -> baseAmount; // waive entirely
        };
    }

    /**
     * Record promo code usage atomically (increments currentUses).
     */
    @Transactional
    public void recordUsage(String promoCodeId, String organizationId, String applicationId,
            String applicantEmail, BigDecimal discountApplied) {
        PromoCode promoCode = getPromoCodeById(promoCodeId);
        promoCode.setCurrentUses(promoCode.getCurrentUses() + 1);
        promoCodeRepository.save(promoCode);

        PromoCodeUsage usage = new PromoCodeUsage();
        usage.setPromoCodeId(promoCodeId);
        usage.setOrganizationId(organizationId);
        usage.setApplicationId(applicationId);
        usage.setApplicantEmail(applicantEmail);
        usage.setDiscountAmount(discountApplied);
        promoCodeUsageRepository.save(usage);
    }

    // ──────────────────────── Private Helpers ────────────────────────

    private void validateDiscountValue(DiscountTypeEnum discountType, BigDecimal discountValue) {
        if (discountType == DiscountTypeEnum.PERCENTAGE) {
            if (discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new BadRequestException("Percentage discount cannot exceed 100%");
            }
        }
        if (discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Discount value must be greater than 0");
        }
    }
}
