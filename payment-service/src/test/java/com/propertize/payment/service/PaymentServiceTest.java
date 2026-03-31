package com.propertize.payment.service;

import com.propertize.payment.config.PaymentConfigProperties;
import com.propertize.payment.dto.payment.request.*;
import com.propertize.payment.dto.payment.response.StripePaymentIntentResponse;
import com.propertize.payment.dto.promo.PromoCodeValidateResponse;
import com.propertize.payment.entity.Payment;
import com.propertize.payment.entity.TransactionHistory;
import com.propertize.payment.enums.*;
import com.propertize.payment.exception.BadRequestException;
import com.propertize.payment.exception.ResourceNotFoundException;
import com.propertize.payment.repository.PaymentRepository;
import com.propertize.payment.repository.TransactionHistoryRepository;
import com.propertize.payment.service.payment.StripePaymentService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Tests")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;
    @Mock
    private StripePaymentService stripePaymentService;
    @Mock
    private PromoCodeService promoCodeService;
    @Mock
    private PaymentConfigProperties paymentConfigProperties;

    @InjectMocks
    private PaymentService paymentService;

    private Payment samplePayment;
    private static final String ORG_ID = "org-001";
    private static final String TENANT_ID = "tenant-001";
    private static final String LEASE_ID = "lease-001";
    private static final String PAYMENT_ID = "pay-001";

    @BeforeEach
    void setUp() {
        PaymentConfigProperties.Stripe stripeConfig = new PaymentConfigProperties.Stripe();
        lenient().when(paymentConfigProperties.getStripe()).thenReturn(stripeConfig);

        samplePayment = new Payment();
        samplePayment.setId(PAYMENT_ID);
        samplePayment.setOrganizationId(ORG_ID);
        samplePayment.setTenantId(TENANT_ID);
        samplePayment.setLeaseId(LEASE_ID);
        samplePayment.setAmount(new BigDecimal("1200.00"));
        samplePayment.setNetAmount(new BigDecimal("1200.00"));
        samplePayment.setPaymentDate(LocalDate.now());
        samplePayment.setPaymentCategory(PaymentCategoryEnum.TENANT_PAYMENT);
        samplePayment.setPaymentContext(PaymentContextEnum.TENANT);
        samplePayment.setStatus(PaymentStatusEnum.PENDING);
        samplePayment.setPaymentGateway(PaymentGatewayEnum.STRIPE);
    }

    // ──────────────────────── getAllPayments ────────────────────────

    @Nested
    @DisplayName("getAllPayments")
    class GetAllPayments {

        @Test
        @DisplayName("Should return paginated payments for organization")
        void shouldReturnPaginatedPayments() {
            Page<Payment> page = new PageImpl<>(List.of(samplePayment));
            when(paymentRepository.findByOrganizationId(eq(ORG_ID), any(Pageable.class)))
                    .thenReturn(page);

            Page<Payment> result = paymentService.getAllPayments(ORG_ID, 1, 20);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getOrganizationId()).isEqualTo(ORG_ID);
            verify(paymentRepository).findByOrganizationId(eq(ORG_ID), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty page when no payments found")
        void shouldReturnEmptyPage() {
            when(paymentRepository.findByOrganizationId(eq(ORG_ID), any(Pageable.class)))
                    .thenReturn(Page.empty());

            Page<Payment> result = paymentService.getAllPayments(ORG_ID, 1, 20);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // ──────────────────────── getPaymentById ────────────────────────

    @Nested
    @DisplayName("getPaymentById")
    class GetPaymentById {

        @Test
        @DisplayName("Should return payment when found")
        void shouldReturnPayment() {
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(samplePayment));

            Payment result = paymentService.getPaymentById(PAYMENT_ID);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(PAYMENT_ID);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(paymentRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> paymentService.getPaymentById("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ──────────────────────── getPaymentsByTenant ────────────────────────

    @Test
    @DisplayName("Should return payments by tenant")
    void shouldReturnPaymentsByTenant() {
        when(paymentRepository.findByTenantId(TENANT_ID)).thenReturn(List.of(samplePayment));

        List<Payment> result = paymentService.getPaymentsByTenant(TENANT_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantId()).isEqualTo(TENANT_ID);
    }

    // ──────────────────────── getPaymentsByLease ────────────────────────

    @Test
    @DisplayName("Should return payments by lease")
    void shouldReturnPaymentsByLease() {
        when(paymentRepository.findByLeaseId(LEASE_ID)).thenReturn(List.of(samplePayment));

        List<Payment> result = paymentService.getPaymentsByLease(LEASE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLeaseId()).isEqualTo(LEASE_ID);
    }

    // ──────────────────────── createPayment ────────────────────────

    @Nested
    @DisplayName("createPayment")
    class CreatePayment {

        @Test
        @DisplayName("Should create payment without promo code")
        void shouldCreatePaymentWithoutPromo() {
            PaymentCreateRequest request = new PaymentCreateRequest();
            request.setOrganizationId(ORG_ID);
            request.setTenantId(TENANT_ID);
            request.setLeaseId(LEASE_ID);
            request.setAmount(new BigDecimal("1200.00"));
            request.setPaymentDate(LocalDate.now());
            request.setPaymentCategory(PaymentCategoryEnum.TENANT_PAYMENT);
            request.setPaymentContext(PaymentContextEnum.TENANT);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
                Payment p = inv.getArgument(0);
                p.setId(PAYMENT_ID);
                return p;
            });

            Payment result = paymentService.createPayment(request);

            assertThat(result.getOrganizationId()).isEqualTo(ORG_ID);
            assertThat(result.getStatus()).isEqualTo(PaymentStatusEnum.PENDING);
            assertThat(result.getNetAmount()).isEqualByComparingTo("1200.00");
            assertThat(result.getPaymentGateway()).isEqualTo(PaymentGatewayEnum.STRIPE);
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("Should create payment with valid promo code applied")
        void shouldCreatePaymentWithPromoCode() {
            PaymentCreateRequest request = new PaymentCreateRequest();
            request.setOrganizationId(ORG_ID);
            request.setTenantId(TENANT_ID);
            request.setAmount(new BigDecimal("1000.00"));
            request.setPaymentDate(LocalDate.now());
            request.setPaymentCategory(PaymentCategoryEnum.TENANT_PAYMENT);
            request.setPaymentContext(PaymentContextEnum.TENANT);
            request.setPromoCode("SAVE10");

            PromoCodeValidateResponse validateResp = new PromoCodeValidateResponse();
            validateResp.setValid(true);
            validateResp.setPromoCodeId("promo-001");

            when(promoCodeService.validatePromoCode(any())).thenReturn(validateResp);
            when(promoCodeService.calculateDiscount("promo-001", new BigDecimal("1000.00")))
                    .thenReturn(new BigDecimal("100.00"));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            Payment result = paymentService.createPayment(request);

            assertThat(result.getDiscountAmount()).isEqualByComparingTo("100.00");
            assertThat(result.getNetAmount()).isEqualByComparingTo("900.00");
        }

        @Test
        @DisplayName("Should gracefully handle invalid promo code")
        void shouldHandleInvalidPromoCode() {
            PaymentCreateRequest request = new PaymentCreateRequest();
            request.setOrganizationId(ORG_ID);
            request.setAmount(new BigDecimal("500.00"));
            request.setPaymentDate(LocalDate.now());
            request.setPaymentCategory(PaymentCategoryEnum.TENANT_PAYMENT);
            request.setPaymentContext(PaymentContextEnum.TENANT);
            request.setPromoCode("INVALID");

            PromoCodeValidateResponse validateResp = new PromoCodeValidateResponse();
            validateResp.setValid(false);
            validateResp.setMessage("Promo code not found");

            when(promoCodeService.validatePromoCode(any())).thenReturn(validateResp);
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            Payment result = paymentService.createPayment(request);

            assertThat(result.getDiscountAmount()).isNull();
            assertThat(result.getNetAmount()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("Should default payment date to today when null")
        void shouldDefaultPaymentDate() {
            PaymentCreateRequest request = new PaymentCreateRequest();
            request.setOrganizationId(ORG_ID);
            request.setAmount(new BigDecimal("100.00"));
            request.setPaymentDate(null);
            request.setPaymentCategory(PaymentCategoryEnum.TENANT_PAYMENT);
            request.setPaymentContext(PaymentContextEnum.TENANT);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            Payment result = paymentService.createPayment(request);

            assertThat(result.getPaymentDate()).isEqualTo(LocalDate.now());
        }
    }

    // ──────────────────────── processPayment ────────────────────────

    @Nested
    @DisplayName("processPayment")
    class ProcessPayment {

        @Test
        @DisplayName("Should process payment successfully via Stripe")
        void shouldProcessPaymentSuccessfully() {
            samplePayment.setNetAmount(new BigDecimal("1200.00"));
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(samplePayment));

            StripePaymentIntentResponse intentResp = new StripePaymentIntentResponse();
            intentResp.setId("pi_test_123");
            intentResp.setStatus("requires_confirmation");
            when(stripePaymentService.createPaymentIntent(any())).thenReturn(intentResp);

            StripePaymentIntentResponse confirmedResp = new StripePaymentIntentResponse();
            confirmedResp.setId("pi_test_123");
            confirmedResp.setStatus("succeeded");
            when(stripePaymentService.confirmPaymentIntent(eq("pi_test_123"), any()))
                    .thenReturn(confirmedResp);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(transactionHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PaymentProcessRequest request = new PaymentProcessRequest();
            request.setStripePaymentMethodId("pm_test_456");

            Payment result = paymentService.processPayment(PAYMENT_ID, request);

            assertThat(result.getStatus()).isEqualTo(PaymentStatusEnum.COMPLETED);
            assertThat(result.getStripePaymentIntentId()).isEqualTo("pi_test_123");
            verify(transactionHistoryRepository).save(any(TransactionHistory.class));
        }

        @Test
        @DisplayName("Should throw when processing already completed payment")
        void shouldThrowWhenAlreadyCompleted() {
            samplePayment.setStatus(PaymentStatusEnum.COMPLETED);
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(samplePayment));

            PaymentProcessRequest request = new PaymentProcessRequest();

            assertThatThrownBy(() -> paymentService.processPayment(PAYMENT_ID, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already completed");
        }

        @Test
        @DisplayName("Should mark payment as FAILED when Stripe confirmation fails")
        void shouldMarkAsFailedWhenConfirmationFails() {
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(samplePayment));

            StripePaymentIntentResponse intentResp = new StripePaymentIntentResponse();
            intentResp.setId("pi_test_123");
            when(stripePaymentService.createPaymentIntent(any())).thenReturn(intentResp);

            StripePaymentIntentResponse failedResp = new StripePaymentIntentResponse();
            failedResp.setId("pi_test_123");
            failedResp.setStatus("failed");
            when(stripePaymentService.confirmPaymentIntent(eq("pi_test_123"), any()))
                    .thenReturn(failedResp);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentProcessRequest request = new PaymentProcessRequest();
            request.setStripePaymentMethodId("pm_test");

            Payment result = paymentService.processPayment(PAYMENT_ID, request);

            assertThat(result.getStatus()).isEqualTo(PaymentStatusEnum.FAILED);
            assertThat(result.getFailureReason()).contains("failed");
        }

        @Test
        @DisplayName("Should handle requires_action status from Stripe")
        void shouldHandleRequiresAction() {
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(samplePayment));

            StripePaymentIntentResponse intentResp = new StripePaymentIntentResponse();
            intentResp.setId("pi_test_123");
            when(stripePaymentService.createPaymentIntent(any())).thenReturn(intentResp);

            StripePaymentIntentResponse actionResp = new StripePaymentIntentResponse();
            actionResp.setId("pi_test_123");
            actionResp.setStatus("requires_action");
            when(stripePaymentService.confirmPaymentIntent(any(), any())).thenReturn(actionResp);

            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentProcessRequest request = new PaymentProcessRequest();

            Payment result = paymentService.processPayment(PAYMENT_ID, request);

            assertThat(result.getStatus()).isEqualTo(PaymentStatusEnum.PENDING);
        }
    }

    // ──────────────────────── refundPayment ────────────────────────

    @Nested
    @DisplayName("refundPayment")
    class RefundPayment {

        @Test
        @DisplayName("Should refund completed payment successfully")
        void shouldRefundSuccessfully() {
            samplePayment.setStatus(PaymentStatusEnum.COMPLETED);
            samplePayment.setStripePaymentIntentId("pi_test_123");
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(samplePayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(transactionHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            PaymentRefundRequest request = new PaymentRefundRequest();
            request.setRefundAmount(new BigDecimal("1200.00"));
            request.setReason("Customer request");

            Payment result = paymentService.refundPayment(PAYMENT_ID, request);

            assertThat(result.getStatus()).isEqualTo(PaymentStatusEnum.REFUNDED);
            verify(stripePaymentService).createRefund(any(StripeRefundRequest.class));
            verify(transactionHistoryRepository).save(any(TransactionHistory.class));
        }

        @Test
        @DisplayName("Should throw when refunding non-completed payment")
        void shouldThrowWhenNotCompleted() {
            samplePayment.setStatus(PaymentStatusEnum.PENDING);
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(samplePayment));

            PaymentRefundRequest request = new PaymentRefundRequest();

            assertThatThrownBy(() -> paymentService.refundPayment(PAYMENT_ID, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Only completed payments can be refunded");
        }

        @Test
        @DisplayName("Should throw when payment has no Stripe intent")
        void shouldThrowWhenNoStripeIntent() {
            samplePayment.setStatus(PaymentStatusEnum.COMPLETED);
            samplePayment.setStripePaymentIntentId(null);
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(samplePayment));

            PaymentRefundRequest request = new PaymentRefundRequest();

            assertThatThrownBy(() -> paymentService.refundPayment(PAYMENT_ID, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("no associated Stripe PaymentIntent");
        }
    }

    // ──────────────────────── updatePayment ────────────────────────

    @Nested
    @DisplayName("updatePayment")
    class UpdatePayment {

        @Test
        @DisplayName("Should update payment status and notes")
        void shouldUpdateStatusAndNotes() {
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(samplePayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentUpdateRequest request = new PaymentUpdateRequest();
            request.setStatus(PaymentStatusEnum.CANCELLED);
            request.setNotes("Cancelled by admin");

            Payment result = paymentService.updatePayment(PAYMENT_ID, request);

            assertThat(result.getStatus()).isEqualTo(PaymentStatusEnum.CANCELLED);
            assertThat(result.getNotes()).isEqualTo("Cancelled by admin");
        }

        @Test
        @DisplayName("Should update only notes when status is null")
        void shouldUpdateOnlyNotes() {
            when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(samplePayment));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

            PaymentUpdateRequest request = new PaymentUpdateRequest();
            request.setNotes("Updated notes");

            Payment result = paymentService.updatePayment(PAYMENT_ID, request);

            assertThat(result.getStatus()).isEqualTo(PaymentStatusEnum.PENDING); // unchanged
            assertThat(result.getNotes()).isEqualTo("Updated notes");
        }
    }
}
