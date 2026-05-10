package com.propertize.payment.service.gdpr;

import com.propertize.payment.dto.gdpr.DataExportResponse;
import com.propertize.payment.entity.Payment;
import com.propertize.payment.entity.PaymentMethod;
import com.propertize.payment.repository.PaymentMethodRepository;
import com.propertize.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GDPR Data Subject Access Request (DSAR) Service
 * 
 * Implements right to data access (GDPR Article 15):
 * - Export all payment data for a user/organization
 * - Include payment records, methods, transaction history
 * - Format: JSON or CSV
 * - Response time: < 30 days per GDPR
 * 
 * Security: Requires user to access only their own data or admin role
 */
@Service
public class GdprDataExportService {

    private static final Logger log = LoggerFactory.getLogger(GdprDataExportService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public GdprDataExportService(PaymentRepository paymentRepository,
            PaymentMethodRepository paymentMethodRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    /**
     * Export all payment data for a user (DSAR)
     * Includes: payments, payment methods, transaction history
     * Note: Payment entity stores ownerId and processedByUserId
     */
    @Transactional(readOnly = true)
    public DataExportResponse exportUserData(UUID userId, UUID organizationId) {
        log.info("GDPR DSAR initiated for user: {}, org: {}", userId, organizationId);

        // Fetch all payment data for this organization
        List<Payment> allPayments = paymentRepository.findAllByOrganizationId(organizationId.toString());
        
        // Filter payments related to this user (owner or processor)
        Long userIdLong = userId == null ? null : Long.parseLong(userId.toString().replaceAll("[^0-9]", ""));
        List<Payment> payments = allPayments.stream()
            .filter(p -> (p.getOwnerId() != null && p.getOwnerId().equals(userIdLong)) ||
                        (p.getProcessedByUserId() != null && p.getProcessedByUserId().equals(userIdLong)))
            .collect(Collectors.toList());
        
        List<PaymentMethod> methods = paymentMethodRepository.findByOrganizationId(organizationId.toString());

        log.info("Exporting {} payments and {} payment methods", payments.size(), methods.size());

        DataExportResponse response = new DataExportResponse();
        response.setRequestedAt(LocalDateTime.now());
        response.setUserId(userId);
        response.setOrganizationId(organizationId);
        response.setPaymentRecords(payments.stream()
                .map(p -> {
                    DataExportResponse.PaymentRecord record = new DataExportResponse.PaymentRecord();
                    record.setId(parseUuid(p.getId()));
                    record.setStripePaymentIntentId(p.getStripePaymentIntentId());
                    record.setAmount(p.getAmount());
                    record.setCurrency("usd");
                    record.setStatus(p.getStatus() != null ? p.getStatus().name() : null);
                    return record;
                })
                .collect(Collectors.toList()));
        response.setPaymentMethods(methods.stream()
                .map(m -> {
                    DataExportResponse.PaymentMethodRecord record = new DataExportResponse.PaymentMethodRecord();
                    record.setId(parseUuid(m.getId()));
                    record.setStripePaymentMethodId(m.getStripePaymentMethodId());
                    record.setCardBrand(m.getCardBrand() != null ? m.getCardBrand().name() : null);
                    record.setLastFour(m.getLastFour());
                    record.setExpMonth(m.getExpMonth() != null ? String.valueOf(m.getExpMonth()) : null);
                    record.setExpYear(m.getExpYear() != null ? String.valueOf(m.getExpYear()) : null);
                    record.setBillingName(m.getCardholderName());
                    return record;
                })
                .collect(Collectors.toList()));
        response.setTotalRecords(payments.size() + methods.size());

        log.info("✓ DSAR export completed: {} total records", response.getTotalRecords());
        return response;
    }

    /**
     * Generate CSV export for user data
     */
    public String exportAsCSV(UUID userId, UUID organizationId) {
        DataExportResponse data = exportUserData(userId, organizationId);

        StringBuilder csv = new StringBuilder();
        csv.append("Payment Records\n");
        csv.append("ID,Stripe Intent ID,Amount,Currency,Status,Created At,Updated At\n");

        for (var record : data.getPaymentRecords()) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                    record.getId(),
                    record.getStripePaymentIntentId(),
                    record.getAmount(),
                    record.getCurrency(),
                    record.getStatus(),
                    record.getCreatedAt(),
                    record.getUpdatedAt()));
        }

        csv.append("\n\nPayment Methods\n");
        csv.append("ID,Card Brand,Last Four,Exp Month,Exp Year,Billing Name,Created At\n");

        for (var method : data.getPaymentMethods()) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                    method.getId(),
                    method.getCardBrand(),
                    method.getLastFour(),
                    method.getExpMonth(),
                    method.getExpYear(),
                    method.getBillingName(),
                    method.getCreatedAt()));
        }

        return csv.toString();
    }

    private UUID parseUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
