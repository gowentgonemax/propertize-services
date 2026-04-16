package com.propertize.payment.service;

import com.propertize.payment.dto.payment.request.OwnerPayoutRequest;
import com.propertize.payment.dto.payment.request.PlatformSubscriptionPaymentRequest;
import com.propertize.payment.dto.payment.request.VendorPaymentRequest;
import com.propertize.payment.entity.Payment;
import com.propertize.commons.enums.payment.PaymentCategoryEnum;
import com.propertize.commons.enums.payment.PaymentContextEnum;
import com.propertize.commons.enums.payment.PaymentStatusEnum;
import com.propertize.payment.repository.PaymentRepository;
import com.propertize.payment.service.payment.StripePaymentService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentContextService Tests")
class PaymentContextServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private StripePaymentService stripePaymentService;

    @InjectMocks
    private PaymentContextService paymentContextService;

    private static final String ORG_ID = "org-001";

    // ─── createVendorPayment ─────────────────────────────────────────────────

    @Nested
    @DisplayName("createVendorPayment")
    class CreateVendorPayment {

        @Test
        @DisplayName("Should create vendor payment with correct fields")
        void shouldCreateVendorPayment() {
            VendorPaymentRequest req = new VendorPaymentRequest();
            req.setOrganizationId(ORG_ID);
            req.setVendorId("vendor-001");
            req.setMaintenanceRequestId("maint-001");
            req.setAmount(new BigDecimal("250.00"));
            req.setDescription("Plumbing repair");
            req.setNotes("Urgent fix");

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId("pay-v001");
                return p;
            });

            Payment result = paymentContextService.createVendorPayment(req);

            assertThat(result.getOrganizationId()).isEqualTo(ORG_ID);
            assertThat(result.getVendorId()).isEqualTo("vendor-001");
            assertThat(result.getMaintenanceRequestId()).isEqualTo("maint-001");
            assertThat(result.getAmount()).isEqualByComparingTo("250.00");
            assertThat(result.getPaymentCategory()).isEqualTo(PaymentCategoryEnum.VENDOR_PAYMENT);
            assertThat(result.getPaymentContext()).isEqualTo(PaymentContextEnum.VENDOR);
            assertThat(result.getStatus()).isEqualTo(PaymentStatusEnum.PENDING);
            assertThat(result.getPaymentDate()).isNotNull();
            verify(paymentRepository).save(any());
        }

        @Test
        @DisplayName("Should set netAmount equal to amount")
        void shouldSetNetAmountEqualToAmount() {
            VendorPaymentRequest req = new VendorPaymentRequest();
            req.setOrganizationId(ORG_ID);
            req.setAmount(new BigDecimal("100.00"));

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            Payment result = paymentContextService.createVendorPayment(req);

            assertThat(result.getNetAmount()).isEqualByComparingTo(result.getAmount());
        }
    }

    // ─── createPlatformSubscriptionPayment ───────────────────────────────────

    @Nested
    @DisplayName("createPlatformSubscriptionPayment")
    class CreatePlatformSubscriptionPayment {

        @Test
        @DisplayName("Should create platform subscription payment with billing period")
        void shouldCreatePlatformSubscriptionPayment() {
            PlatformSubscriptionPaymentRequest req = new PlatformSubscriptionPaymentRequest();
            req.setOrganizationId(ORG_ID);
            req.setAmount(new BigDecimal("99.00"));
            req.setBillingPeriodStart(LocalDate.of(2026, 4, 1));
            req.setBillingPeriodEnd(LocalDate.of(2026, 4, 30));
            req.setNotes("Monthly plan");

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            Payment result = paymentContextService.createPlatformSubscriptionPayment(req);

            assertThat(result.getPaymentCategory()).isEqualTo(PaymentCategoryEnum.PLATFORM_SUBSCRIPTION);
            assertThat(result.getPaymentContext()).isEqualTo(PaymentContextEnum.PLATFORM);
            assertThat(result.getBillingPeriodStart()).isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(result.getBillingPeriodEnd()).isEqualTo(LocalDate.of(2026, 4, 30));
            assertThat(result.getStatus()).isEqualTo(PaymentStatusEnum.PENDING);
        }

        @Test
        @DisplayName("Should set netAmount equal to amount for subscription")
        void shouldSetNetAmountEqualToAmountForSubscription() {
            PlatformSubscriptionPaymentRequest req = new PlatformSubscriptionPaymentRequest();
            req.setOrganizationId(ORG_ID);
            req.setAmount(new BigDecimal("49.99"));

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            Payment result = paymentContextService.createPlatformSubscriptionPayment(req);

            assertThat(result.getNetAmount()).isEqualByComparingTo("49.99");
        }
    }

    // ─── createOwnerPayout ───────────────────────────────────────────────────

    @Nested
    @DisplayName("createOwnerPayout")
    class CreateOwnerPayout {

        @Test
        @DisplayName("Should create owner payout with correct fields")
        void shouldCreateOwnerPayout() {
            OwnerPayoutRequest req = new OwnerPayoutRequest();
            req.setOrganizationId(ORG_ID);
            req.setOwnerId(42L);
            req.setPropertyId("prop-001");
            req.setAmount(new BigDecimal("1500.00"));
            req.setDescription("Monthly rent payout");
            req.setNotes("Net of fees");

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            Payment result = paymentContextService.createOwnerPayout(req);

            assertThat(result.getOwnerId()).isEqualTo(42L);
            assertThat(result.getPropertyId()).isEqualTo("prop-001");
            assertThat(result.getPaymentCategory()).isEqualTo(PaymentCategoryEnum.OWNER_PAYOUT);
            assertThat(result.getPaymentContext()).isEqualTo(PaymentContextEnum.OWNER);
            assertThat(result.getStatus()).isEqualTo(PaymentStatusEnum.PENDING);
            assertThat(result.getAmount()).isEqualByComparingTo("1500.00");
        }

        @Test
        @DisplayName("Should set today as payment date for owner payout")
        void shouldSetTodayAsPaymentDate() {
            OwnerPayoutRequest req = new OwnerPayoutRequest();
            req.setOrganizationId(ORG_ID);
            req.setAmount(new BigDecimal("1000.00"));

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            Payment result = paymentContextService.createOwnerPayout(req);

            assertThat(result.getPaymentDate()).isEqualTo(LocalDate.now());
        }
    }
}
