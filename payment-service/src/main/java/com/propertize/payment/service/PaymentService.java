package com.propertize.payment.service;

import com.propertize.payment.dto.payment.request.*;
import com.propertize.payment.dto.payment.response.StripePaymentIntentResponse;
import com.propertize.payment.entity.Payment;
import com.propertize.payment.entity.TransactionHistory;
import com.propertize.payment.enums.*;
import com.propertize.payment.exception.BadRequestException;
import com.propertize.payment.exception.ResourceNotFoundException;
import com.propertize.payment.repository.PaymentRepository;
import com.propertize.payment.repository.TransactionHistoryRepository;
import com.propertize.payment.service.payment.StripePaymentService;
import com.propertize.payment.util.PaginationValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final StripePaymentService stripePaymentService;
    private final PromoCodeService promoCodeService;

    // ──────────────────────── CRUD ────────────────────────

    public Page<Payment> getAllPayments(String organizationId, int page, int size) {
        Pageable pageable = PaginationValidator.createPageable(page, size, "createdAt", "desc");
        return paymentRepository.findByOrganizationId(organizationId, pageable);
    }

    public Payment getPaymentById(String id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", id));
    }

    public List<Payment> getPaymentsByTenant(String tenantId) {
        return paymentRepository.findByTenantId(tenantId);
    }

    public List<Payment> getPaymentsByLease(String leaseId) {
        return paymentRepository.findByLeaseId(leaseId);
    }

    // ──────────────────────── Create ────────────────────────

    @Transactional
    public Payment createPayment(PaymentCreateRequest request) {
        Payment payment = new Payment();
        payment.setOrganizationId(request.getOrganizationId());
        payment.setTenantId(request.getTenantId());
        payment.setLeaseId(request.getLeaseId());
        payment.setAmount(request.getAmount());
        payment.setPaymentDate(request.getPaymentDate() != null ? request.getPaymentDate() : LocalDate.now());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setPaymentCategory(request.getPaymentCategory());
        payment.setPaymentContext(request.getPaymentContext());
        payment.setPaymentType(request.getPaymentType());
        payment.setPaymentGateway(PaymentGatewayEnum.STRIPE);
        payment.setStatus(PaymentStatusEnum.PENDING);
        payment.setNotes(request.getNotes());

        // Apply promo code discount if provided
        if (request.getPromoCode() != null && !request.getPromoCode().isBlank()) {
            applyPromoCode(payment, request.getPromoCode(), request.getOrganizationId(), null, null);
        }

        recalculateNetAmount(payment);
        payment.setPromoCode(request.getPromoCode());
        return paymentRepository.save(payment);
    }

    // ──────────────────────── Process Payment (Stripe) ────────────────────────

    @Transactional
    public Payment processPayment(String paymentId, PaymentProcessRequest request) {
        Payment payment = getPaymentById(paymentId);

        if (payment.getStatus() == PaymentStatusEnum.COMPLETED) {
            throw new BadRequestException("Payment " + paymentId + " is already completed");
        }

        StripePaymentIntentRequest intentRequest = new StripePaymentIntentRequest();
        intentRequest.setAmount(payment.getNetAmount());
        intentRequest.setCurrency("usd");
        intentRequest.setPaymentMethodId(request.getStripePaymentMethodId());
        intentRequest.setDescription("Payment #" + paymentId);

        StripePaymentIntentResponse intentResponse = stripePaymentService.createPaymentIntent(intentRequest);
        payment.setStripePaymentIntentId(intentResponse.getId());
        payment.setStatus(PaymentStatusEnum.PROCESSING);

        // confirm immediately
        StripePaymentIntentResponse confirmed = stripePaymentService.confirmPaymentIntent(
                intentResponse.getId(), request.getStripePaymentMethodId());

        if ("succeeded".equals(confirmed.getStatus())) {
            payment.setStatus(PaymentStatusEnum.COMPLETED);
            recordTransaction(payment, TransactionTypeEnum.RENT_PAYMENT, TransactionStatusEnum.SUCCESS,
                    intentResponse.getId());
        } else if ("requires_action".equals(confirmed.getStatus())) {
            payment.setStatus(PaymentStatusEnum.PENDING);
        } else {
            payment.setStatus(PaymentStatusEnum.FAILED);
            payment.setFailureReason("Payment confirmation status: " + confirmed.getStatus());
        }

        return paymentRepository.save(payment);
    }

    // ──────────────────────── Refund ────────────────────────

    @Transactional
    public Payment refundPayment(String paymentId, PaymentRefundRequest request) {
        Payment payment = getPaymentById(paymentId);

        if (payment.getStatus() != PaymentStatusEnum.COMPLETED) {
            throw new BadRequestException("Only completed payments can be refunded");
        }
        if (payment.getStripePaymentIntentId() == null) {
            throw new BadRequestException("Payment has no associated Stripe PaymentIntent");
        }

        StripeRefundRequest refundRequest = new StripeRefundRequest();
        refundRequest.setPaymentIntentId(payment.getStripePaymentIntentId());
        if (request.getRefundAmount() != null) {
            refundRequest.setAmount(request.getRefundAmount());
        }
        refundRequest.setReason(request.getReason() != null ? "requested_by_customer" : null);

        stripePaymentService.createRefund(refundRequest);

        payment.setStatus(PaymentStatusEnum.REFUNDED);
        payment.setNotes(request.getReason());

        recordTransaction(payment, TransactionTypeEnum.REFUND, TransactionStatusEnum.REFUNDED,
                payment.getStripePaymentIntentId());

        return paymentRepository.save(payment);
    }

    // ──────────────────────── Update ────────────────────────

    @Transactional
    public Payment updatePayment(String id, PaymentUpdateRequest request) {
        Payment payment = getPaymentById(id);
        if (request.getStatus() != null) {
            payment.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            payment.setNotes(request.getNotes());
        }
        return paymentRepository.save(payment);
    }

    // ──────────────────────── Helpers ────────────────────────

    private void applyPromoCode(Payment payment, String code, String organizationId,
            String applicationId, String applicantEmail) {
        try {
            var validateRequest = new com.propertize.payment.dto.promo.PromoCodeValidateRequest();
            validateRequest.setCode(code);
            validateRequest.setOrganizationId(organizationId);
            validateRequest.setApplicationId(applicationId);
            validateRequest.setApplicantEmail(applicantEmail);

            var result = promoCodeService.validatePromoCode(validateRequest);
            if (result.isValid()) {
                BigDecimal discount = promoCodeService.calculateDiscount(
                        result.getPromoCodeId(), payment.getAmount());
                payment.setDiscountAmount(discount);
            }
        } catch (Exception e) {
            log.warn("Promo code '{}' validation failed: {}", code, e.getMessage());
        }
    }

    private void recalculateNetAmount(Payment payment) {
        BigDecimal amount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
        BigDecimal discount = payment.getDiscountAmount() != null ? payment.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal lateFee = payment.getLateFee() != null ? payment.getLateFee() : BigDecimal.ZERO;
        payment.setNetAmount(amount.subtract(discount).add(lateFee));
    }

    private void recordTransaction(Payment payment, TransactionTypeEnum type,
            TransactionStatusEnum status, String providerRefId) {
        TransactionHistory txn = new TransactionHistory();
        txn.setOrganizationId(payment.getOrganizationId());
        txn.setPaymentId(payment.getId());
        txn.setTenantId(payment.getTenantId());
        txn.setLeaseId(payment.getLeaseId());
        txn.setAmount(payment.getNetAmount());
        txn.setCurrency("USD");
        txn.setTransactionType(type);
        txn.setStatus(status);
        txn.setProviderReferenceId(providerRefId);
        txn.setPaymentGateway("STRIPE");
        txn.setTransactionDate(LocalDateTime.now());
        transactionHistoryRepository.save(txn);
    }
}
