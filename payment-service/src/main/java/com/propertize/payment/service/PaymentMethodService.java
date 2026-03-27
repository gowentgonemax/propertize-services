package com.propertize.payment.service;

import com.propertize.payment.dto.payment.request.CreateACHPaymentMethodRequest;
import com.propertize.payment.dto.payment.request.CreateStripePaymentMethodRequest;
import com.propertize.payment.entity.PaymentMethod;
import com.propertize.payment.enums.PaymentMethodEnum;
import com.propertize.payment.exception.BadRequestException;
import com.propertize.payment.exception.ResourceNotFoundException;
import com.propertize.payment.repository.PaymentMethodRepository;
import com.propertize.payment.service.payment.StripePaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final StripePaymentService stripePaymentService;

    public List<PaymentMethod> getPaymentMethodsByTenant(String tenantId) {
        return paymentMethodRepository.findByTenantIdAndIsActiveTrue(tenantId);
    }

    public PaymentMethod getPaymentMethodById(String id) {
        return paymentMethodRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentMethod", "id", id));
    }

    @Transactional
    public PaymentMethod addCardPaymentMethod(CreateStripePaymentMethodRequest request) {
        // Dedup by fingerprint within org
        if (request.getFingerprint() != null) {
            paymentMethodRepository.findByFingerprintAndOrganizationId(
                    request.getFingerprint(), request.getOrganizationId())
                    .ifPresent(existing -> {
                        throw new BadRequestException("This card is already on file");
                    });
        }

        // Attach to Stripe customer
        if (request.getStripeCustomerId() != null && request.getStripePaymentMethodId() != null) {
            stripePaymentService.attachPaymentMethod(
                    request.getStripePaymentMethodId(), request.getStripeCustomerId());
        }

        PaymentMethod pm = new PaymentMethod();
        pm.setOrganizationId(request.getOrganizationId());
        pm.setTenantId(request.getTenantId());
        pm.setMethodType(PaymentMethodEnum.CREDIT_CARD);
        pm.setStripePaymentMethodId(request.getStripePaymentMethodId());
        pm.setStripeCustomerId(request.getStripeCustomerId());
        pm.setCardBrand(request.getBrand() != null
                ? com.propertize.payment.enums.CardBrandEnum.valueOf(request.getBrand().toUpperCase())
                : null);
        pm.setLastFour(request.getLastFour());
        pm.setExpMonth(request.getExpMonth());
        pm.setExpYear(request.getExpYear());
        pm.setCardholderName(request.getCardholderName());
        pm.setFingerprint(request.getFingerprint());
        pm.setIsActive(true);
        pm.setIsDefault(false);

        // First card becomes default
        if (paymentMethodRepository.findByTenantIdAndIsActiveTrue(request.getTenantId()).isEmpty()) {
            pm.setIsDefault(true);
        }

        return paymentMethodRepository.save(pm);
    }

    @Transactional
    public PaymentMethod addACHPaymentMethod(CreateACHPaymentMethodRequest request) {
        if (request.getStripeCustomerId() != null && request.getStripePaymentMethodId() != null) {
            stripePaymentService.attachPaymentMethod(
                    request.getStripePaymentMethodId(), request.getStripeCustomerId());
        }

        PaymentMethod pm = new PaymentMethod();
        pm.setOrganizationId(request.getOrganizationId());
        pm.setTenantId(request.getTenantId());
        pm.setMethodType(PaymentMethodEnum.ACH);
        pm.setStripePaymentMethodId(request.getStripePaymentMethodId());
        pm.setStripeCustomerId(request.getStripeCustomerId());
        pm.setBankName(request.getBankName());
        pm.setBankAccountType(request.getAccountType());
        pm.setLastFour(request.getLastFour());
        pm.setIsActive(true);
        pm.setIsDefault(false);

        if (paymentMethodRepository.findByTenantIdAndIsActiveTrue(request.getTenantId()).isEmpty()) {
            pm.setIsDefault(true);
        }

        return paymentMethodRepository.save(pm);
    }

    @Transactional
    public void setDefaultPaymentMethod(String id, String tenantId) {
        PaymentMethod target = getPaymentMethodById(id);
        // Remove current default
        paymentMethodRepository.findByTenantIdAndIsActiveTrue(tenantId)
                .stream()
                .filter(PaymentMethod::getIsDefault)
                .forEach(pm -> {
                    pm.setIsDefault(false);
                    paymentMethodRepository.save(pm);
                });
        target.setIsDefault(true);
        paymentMethodRepository.save(target);
    }

    @Transactional
    public void deletePaymentMethod(String id) {
        PaymentMethod pm = getPaymentMethodById(id);
        if (pm.getStripePaymentMethodId() != null) {
            stripePaymentService.detachPaymentMethod(pm.getStripePaymentMethodId());
        }
        pm.setIsActive(false);
        paymentMethodRepository.save(pm);
    }
}
