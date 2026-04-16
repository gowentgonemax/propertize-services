package com.propertize.payment.service;

import com.propertize.payment.config.StripeConfig;
import com.propertize.payment.dto.payment.request.ApplicationFeeProcessPaymentRequest;
import com.propertize.payment.dto.payment.request.ApplicationFeeRequest;
import com.propertize.payment.dto.payment.request.StripePaymentIntentRequest;
import com.propertize.payment.dto.payment.response.StripePaymentIntentResponse;
import com.propertize.payment.dto.promo.PromoCodeValidateRequest;
import com.propertize.payment.dto.promo.PromoCodeValidateResponse;
import com.propertize.payment.entity.ApplicationFee;
import com.propertize.commons.enums.payment.PaymentStatusEnum;
import com.propertize.commons.exception.BadRequestException;
import com.propertize.commons.exception.ResourceNotFoundException;
import com.propertize.payment.repository.ApplicationFeeRepository;
import com.propertize.payment.service.payment.StripePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationFeeService {

    private final ApplicationFeeRepository applicationFeeRepository;
    private final StripePaymentService stripePaymentService;
    private final PromoCodeService promoCodeService;
    private final StripeConfig stripeConfig;

    public ApplicationFee getApplicationFeeById(String id) {
        return applicationFeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApplicationFee", "id", id));
    }

    public ApplicationFee getApplicationFeeByRentalApplicationId(String rentalApplicationId) {
        return applicationFeeRepository.findByRentalApplicationId(rentalApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("ApplicationFee", "rentalApplicationId",
                        rentalApplicationId));
    }

    @Transactional
    public ApplicationFee createApplicationFee(ApplicationFeeRequest request) {
        // Check for duplicate
        applicationFeeRepository.findByRentalApplicationId(request.getRentalApplicationId())
                .ifPresent(existing -> {
                    throw new BadRequestException(
                            "Application fee already exists for application " + request.getRentalApplicationId());
                });

        ApplicationFee fee = new ApplicationFee();
        fee.setOrganizationId(request.getOrganizationId());
        fee.setRentalApplicationId(request.getRentalApplicationId());
        fee.setApplicantEmail(request.getApplicantEmail());
        fee.setFeeAmount(request.getFeeAmount());
        fee.setDueDate(OffsetDateTime.now().plusDays(7));
        fee.setPaymentStatus(PaymentStatusEnum.PENDING);

        BigDecimal finalAmount = request.getFeeAmount();

        // Apply promo code
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            PromoCodeValidateRequest validateRequest = new PromoCodeValidateRequest();
            validateRequest.setCode(request.getPromoCode());
            validateRequest.setOrganizationId(request.getOrganizationId());
            validateRequest.setApplicationId(request.getRentalApplicationId());
            validateRequest.setApplicantEmail(request.getApplicantEmail());

            PromoCodeValidateResponse result = promoCodeService.validatePromoCode(validateRequest);
            if (result.isValid()) {
                BigDecimal discount = promoCodeService.calculateDiscount(result.getPromoCodeId(),
                        request.getFeeAmount());
                fee.setDiscountAmount(discount);
                fee.setPromoCodeUsed(request.getPromoCode().toUpperCase());
                finalAmount = request.getFeeAmount().subtract(discount);
                if (finalAmount.compareTo(BigDecimal.ZERO) < 0)
                    finalAmount = BigDecimal.ZERO;
            }
        }

        fee.setFinalAmount(finalAmount);

        // Create Stripe PaymentIntent
        if (finalAmount.compareTo(BigDecimal.ZERO) > 0) {
            StripePaymentIntentRequest intentRequest = new StripePaymentIntentRequest();
            intentRequest.setAmount(finalAmount);
            intentRequest.setCurrency(stripeConfig.getCurrency());
            StripePaymentIntentResponse intentResponse = stripePaymentService.createPaymentIntent(intentRequest);
            fee.setStripePaymentIntentId(intentResponse.getId());
            fee.setStripeClientSecret(intentResponse.getClientSecret());
        } else {
            // Fee waived by promo code
            fee.setPaymentStatus(PaymentStatusEnum.COMPLETED);
            fee.setPaidAt(LocalDateTime.now());
        }

        ApplicationFee saved = applicationFeeRepository.save(fee);

        // Record promo usage
        if (request.getPromoCode() != null && fee.getDiscountAmount() != null) {
            PromoCodeValidateRequest validateRequest = new PromoCodeValidateRequest();
            validateRequest.setCode(request.getPromoCode());
            validateRequest.setOrganizationId(request.getOrganizationId());
            validateRequest.setApplicationId(request.getRentalApplicationId());
            PromoCodeValidateResponse result = promoCodeService.validatePromoCode(validateRequest);
            if (result.isValid()) {
                promoCodeService.recordUsage(result.getPromoCodeId(), request.getOrganizationId(),
                        request.getRentalApplicationId(), request.getApplicantEmail(), fee.getDiscountAmount());
            }
        }

        return saved;
    }

    @Transactional
    public ApplicationFee processPayment(String id, ApplicationFeeProcessPaymentRequest request) {
        ApplicationFee fee = getApplicationFeeById(id);
        if (fee.getPaymentStatus() == PaymentStatusEnum.COMPLETED) {
            throw new BadRequestException("Fee is already paid");
        }

        if (fee.getStripePaymentIntentId() != null && request.getStripePaymentMethodId() != null) {
            StripePaymentIntentResponse confirmed = stripePaymentService.confirmPaymentIntent(
                    fee.getStripePaymentIntentId(), request.getStripePaymentMethodId());
            if ("succeeded".equals(confirmed.getStatus())) {
                fee.setPaymentStatus(PaymentStatusEnum.COMPLETED);
                fee.setPaidAt(LocalDateTime.now());
            } else {
                fee.setPaymentStatus(PaymentStatusEnum.FAILED);
            }
        }

        return applicationFeeRepository.save(fee);
    }
}
