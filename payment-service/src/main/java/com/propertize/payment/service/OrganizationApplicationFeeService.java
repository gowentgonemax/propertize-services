package com.propertize.payment.service;

import com.propertize.payment.config.StripeConfig;
import com.propertize.payment.dto.payment.request.StripePaymentIntentRequest;
import com.propertize.payment.dto.payment.response.StripePaymentIntentResponse;
import com.propertize.payment.entity.OrganizationApplicationFee;
import com.propertize.commons.enums.payment.PaymentStatusEnum;
import com.propertize.commons.exception.BadRequestException;
import com.propertize.commons.exception.ResourceNotFoundException;
import com.propertize.payment.repository.OrganizationApplicationFeeRepository;
import com.propertize.payment.service.payment.StripePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationApplicationFeeService {

    private final OrganizationApplicationFeeRepository orgApplicationFeeRepository;
    private final StripePaymentService stripePaymentService;
    private final StripeConfig stripeConfig;

    public OrganizationApplicationFee getByTrackingId(String trackingId) {
        return orgApplicationFeeRepository.findByTrackingId(trackingId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("OrganizationApplicationFee", "trackingId", trackingId));
    }

    public OrganizationApplicationFee getById(String id) {
        return orgApplicationFeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrganizationApplicationFee", "id", id));
    }

    /**
     * Initiate org onboarding fee — creates Stripe PaymentIntent.
     * Called before org is fully created (no orgId in context).
     */
    @Transactional
    public OrganizationApplicationFee initiate(String organizationName, String organizationEmail,
            BigDecimal feeAmount) {
        OrganizationApplicationFee fee = new OrganizationApplicationFee();
        fee.setTrackingId(UUID.randomUUID().toString());
        fee.setOrganizationName(organizationName);
        fee.setApplicantEmail(organizationEmail);
        fee.setFeeAmount(feeAmount);
        fee.setPaymentStatus(PaymentStatusEnum.PENDING);

        if (feeAmount.compareTo(BigDecimal.ZERO) > 0) {
            StripePaymentIntentRequest intentRequest = new StripePaymentIntentRequest();
            intentRequest.setAmount(feeAmount);
            intentRequest.setCurrency(stripeConfig.getCurrency());
            StripePaymentIntentResponse intentResponse = stripePaymentService.createPaymentIntent(intentRequest);
            fee.setStripePaymentIntentId(intentResponse.getId());
            fee.setStripeClientSecret(intentResponse.getClientSecret());
        } else {
            fee.setPaymentStatus(PaymentStatusEnum.COMPLETED);
        }

        return orgApplicationFeeRepository.save(fee);
    }

    /**
     * Complete payment after Stripe confirms client-side.
     */
    @Transactional
    public OrganizationApplicationFee completePayment(String trackingId, String stripePaymentMethodId) {
        OrganizationApplicationFee fee = getByTrackingId(trackingId);
        if (fee.getPaymentStatus() == PaymentStatusEnum.COMPLETED) {
            return fee;
        }

        if (fee.getStripePaymentIntentId() != null && stripePaymentMethodId != null) {
            StripePaymentIntentResponse confirmed = stripePaymentService.confirmPaymentIntent(
                    fee.getStripePaymentIntentId(), stripePaymentMethodId);
            if ("succeeded".equals(confirmed.getStatus())) {
                fee.setPaymentStatus(PaymentStatusEnum.COMPLETED);
            } else {
                fee.setPaymentStatus(PaymentStatusEnum.FAILED);
            }
        }

        return orgApplicationFeeRepository.save(fee);
    }

    public boolean isFeePaid(String trackingId) {
        return orgApplicationFeeRepository.findByTrackingId(trackingId)
                .map(fee -> fee.getPaymentStatus() == PaymentStatusEnum.COMPLETED)
                .orElse(false);
    }
}
