package com.propertize.payment.service;

import com.propertize.payment.dto.payment.request.*;
import com.propertize.payment.entity.Payment;
import com.propertize.payment.enums.*;
import com.propertize.payment.exception.BadRequestException;
import com.propertize.payment.repository.PaymentRepository;
import com.propertize.payment.service.payment.StripePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Handles payments in specific business contexts:
 * - Vendor payments (maintenance invoices)
 * - Platform subscription payments
 * - Owner payouts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentContextService {

    private final PaymentRepository paymentRepository;
    private final StripePaymentService stripePaymentService;

    @Transactional
    public Payment createVendorPayment(VendorPaymentRequest request) {
        Payment payment = new Payment();
        payment.setOrganizationId(request.getOrganizationId());
        payment.setVendorId(request.getVendorId());
        payment.setMaintenanceRequestId(request.getMaintenanceRequestId());
        payment.setAmount(request.getAmount());
        payment.setNetAmount(request.getAmount());
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentCategory(PaymentCategoryEnum.VENDOR_PAYMENT);
        payment.setPaymentContext(PaymentContextEnum.VENDOR);
        payment.setPaymentGateway(PaymentGatewayEnum.STRIPE);
        payment.setStatus(PaymentStatusEnum.PENDING);
        payment.setNotes(request.getNotes());
        payment.setDescription(request.getDescription());
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment createPlatformSubscriptionPayment(PlatformSubscriptionPaymentRequest request) {
        Payment payment = new Payment();
        payment.setOrganizationId(request.getOrganizationId());
        payment.setAmount(request.getAmount());
        payment.setNetAmount(request.getAmount());
        payment.setPaymentDate(LocalDate.now());
        payment.setBillingPeriodStart(request.getBillingPeriodStart());
        payment.setBillingPeriodEnd(request.getBillingPeriodEnd());
        payment.setPaymentCategory(PaymentCategoryEnum.PLATFORM_SUBSCRIPTION);
        payment.setPaymentContext(PaymentContextEnum.PLATFORM);
        payment.setPaymentGateway(PaymentGatewayEnum.STRIPE);
        payment.setStatus(PaymentStatusEnum.PENDING);
        payment.setNotes(request.getNotes());
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment createOwnerPayout(OwnerPayoutRequest request) {
        Payment payment = new Payment();
        payment.setOrganizationId(request.getOrganizationId());
        payment.setOwnerId(request.getOwnerId());
        payment.setPropertyId(request.getPropertyId());
        payment.setAmount(request.getAmount());
        payment.setNetAmount(request.getAmount());
        payment.setPaymentDate(LocalDate.now());
        payment.setPaymentCategory(PaymentCategoryEnum.OWNER_PAYOUT);
        payment.setPaymentContext(PaymentContextEnum.OWNER);
        payment.setPaymentGateway(PaymentGatewayEnum.STRIPE);
        payment.setStatus(PaymentStatusEnum.PENDING);
        payment.setNotes(request.getNotes());
        payment.setDescription(request.getDescription());
        return paymentRepository.save(payment);
    }
}
