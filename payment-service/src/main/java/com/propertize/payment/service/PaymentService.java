package com.propertize.payment.service;

import com.propertize.payment.config.PaymentConfigProperties;
import com.propertize.payment.dto.payment.request.*;
import com.propertize.payment.dto.payment.response.PaymentStatisticsResponse;
import com.propertize.payment.dto.payment.response.StripePaymentIntentResponse;
import com.propertize.payment.entity.Payment;
import com.propertize.payment.entity.TransactionHistory;
import com.propertize.commons.enums.payment.PaymentStatusEnum;
import com.propertize.commons.enums.payment.TransactionStatusEnum;
import com.propertize.commons.enums.payment.TransactionTypeEnum;
import com.propertize.payment.enums.PaymentGatewayEnum;
import com.propertize.payment.enums.StripePaymentIntentStatusEnum;
import com.propertize.commons.exception.BadRequestException;
import com.propertize.commons.exception.ResourceNotFoundException;
import com.propertize.payment.repository.PaymentRepository;
import com.propertize.payment.repository.projection.PaymentStatisticsProjection;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final StripePaymentService stripePaymentService;
    private final PromoCodeService promoCodeService;
    private final PaymentConfigProperties paymentConfigProperties;

    // ──────────────────────── CRUD ────────────────────────

    public Page<Payment> getAllPayments(String organizationId, String tenantId, int page, int size) {
        Pageable pageable = PaginationValidator.createPageable(page, size, "createdAt", "desc");
        if (tenantId != null && !tenantId.isBlank()) {
            return paymentRepository.findByOrganizationIdAndTenantId(organizationId, tenantId, pageable);
        }
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
        intentRequest.setCurrency(paymentConfigProperties.getStripe().getCurrency());
        intentRequest.setPaymentMethodId(request.getStripePaymentMethodId());
        intentRequest.setDescription("Payment #" + paymentId);

        StripePaymentIntentResponse intentResponse = stripePaymentService.createPaymentIntent(intentRequest);
        payment.setStripePaymentIntentId(intentResponse.getId());
        payment.setStatus(PaymentStatusEnum.PROCESSING);

        // confirm immediately
        StripePaymentIntentResponse confirmed = stripePaymentService.confirmPaymentIntent(
                intentResponse.getId(), request.getStripePaymentMethodId());

        if (StripePaymentIntentStatusEnum.SUCCEEDED.getStripeValue().equals(confirmed.getStatus())) {
            payment.setStatus(PaymentStatusEnum.COMPLETED);
            recordTransaction(payment, TransactionTypeEnum.RENT_PAYMENT, TransactionStatusEnum.SUCCESS,
                    intentResponse.getId());
        } else if (StripePaymentIntentStatusEnum.REQUIRES_ACTION.getStripeValue().equals(confirmed.getStatus())) {
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
        refundRequest.setReason(request.getReason());

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
        txn.setCurrency(paymentConfigProperties.getStripe().getCurrency().toUpperCase());
        txn.setTransactionType(type);
        txn.setStatus(status);
        txn.setProviderReferenceId(providerRefId);
        txn.setPaymentGateway(PaymentGatewayEnum.STRIPE);
        txn.setTransactionDate(LocalDateTime.now());
        transactionHistoryRepository.save(txn);
    }

    // ──────────────────────── Statistics ─────────────────────────────────────

    /**
     * Returns aggregated payment statistics for an organisation, with optional
     * date-range, tenant, and lease filters.
     *
     * @param organizationId required
     * @param startDate      optional — ISO date string (yyyy-MM-dd)
     * @param endDate        optional — ISO date string (yyyy-MM-dd)
     * @param tenantId       optional
     * @param leaseId        optional
     */
    public PaymentStatisticsResponse getPaymentStatistics(
            String organizationId,
            String startDate,
            String endDate,
            String tenantId,
            String leaseId) {

        PaymentStatisticsProjection proj = paymentRepository.computeStatistics(
                organizationId, startDate, endDate, tenantId, leaseId);

        Map<String, Long> byMethod = buildBreakdown(
                paymentRepository.countByMethod(organizationId, startDate, endDate, tenantId, leaseId));

        Map<String, Long> byCategory = buildBreakdown(
                paymentRepository.countByCategory(organizationId, startDate, endDate, tenantId, leaseId));

        return PaymentStatisticsResponse.from(proj, byMethod, byCategory);
    }

    // ──────────────────────── Due-Soon ───────────────────────────────────────

    /**
     * Returns pending/scheduled payments due within the next {@code days} days.
     *
     * @param organizationId required
     * @param days           look-ahead window in days (default 3)
     */
    public List<Payment> getPaymentsDueSoon(String organizationId, int days) {
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(days);
        return paymentRepository.findPaymentsDueSoon(organizationId, today, maxDate);
    }

    // ──────────────────────── Failed-Retry ───────────────────────────────────

    /**
     * Returns failed payments for the given organisation that are eligible for
     * a retry attempt.
     *
     * @param organizationId required
     */
    public List<Payment> getFailedPaymentsForRetry(String organizationId) {
        return paymentRepository.findFailedPaymentsForRetryByOrg(organizationId);
    }

    // ──────────────────────── Recurring-Due ──────────────────────────────────

    /**
     * Returns recurring payments (type = RECURRING_FEE) whose due date is on
     * or before the supplied date.
     *
     * @param organizationId required
     * @param dueDate        reference date; defaults to today when null
     */
    public List<Payment> getRecurringPaymentsDue(String organizationId, LocalDate dueDate) {
        LocalDate effectiveDate = dueDate != null ? dueDate : LocalDate.now();
        return paymentRepository.findRecurringPaymentsDue(organizationId, effectiveDate);
    }

    // ──────────────────────── Private helpers ────────────────────────────────

    /**
     * Converts native {@code Object[]} rows (col-0 = label, col-1 = count)
     * into an insertion-ordered map.
     */
    private Map<String, Long> buildBreakdown(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String key = row[0] != null ? row[0].toString() : "UNKNOWN";
            long cnt = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            result.put(key, cnt);
        }
        return result;
    }
}
