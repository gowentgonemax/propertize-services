package com.propertize.payment.service;

import com.propertize.payment.config.StripeConfig;
import com.propertize.payment.dto.payment.response.StripePaymentIntentResponse;
import com.propertize.payment.entity.OrganizationApplicationFee;
import com.propertize.commons.enums.payment.PaymentStatusEnum;
import com.propertize.commons.exception.ResourceNotFoundException;
import com.propertize.payment.repository.OrganizationApplicationFeeRepository;
import com.propertize.payment.service.payment.StripePaymentService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationApplicationFeeService Tests")
class OrganizationApplicationFeeServiceTest {

    @Mock
    private OrganizationApplicationFeeRepository orgApplicationFeeRepository;
    @Mock
    private StripePaymentService stripePaymentService;
    @Mock
    private StripeConfig stripeConfig;

    @InjectMocks
    private OrganizationApplicationFeeService orgApplicationFeeService;

    private static final String TRACKING_ID = "tracking-001";
    private static final String FEE_ID = "fee-001";

    private OrganizationApplicationFee sampleFee;

    @BeforeEach
    void setUp() {
        sampleFee = new OrganizationApplicationFee();
        sampleFee.setId(FEE_ID);
        sampleFee.setTrackingId(TRACKING_ID);
        sampleFee.setOrganizationName("Test Org LLC");
        sampleFee.setApplicantEmail("org@example.com");
        sampleFee.setFeeAmount(new BigDecimal("100.00"));
        sampleFee.setPaymentStatus(PaymentStatusEnum.PENDING);
    }

    // ─── getByTrackingId ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getByTrackingId")
    class GetByTrackingId {

        @Test
        @DisplayName("Should return fee when found by tracking id")
        void shouldReturnFeeByTrackingId() {
            when(orgApplicationFeeRepository.findByTrackingId(TRACKING_ID))
                    .thenReturn(Optional.of(sampleFee));

            OrganizationApplicationFee result = orgApplicationFeeService.getByTrackingId(TRACKING_ID);

            assertThat(result.getTrackingId()).isEqualTo(TRACKING_ID);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found by tracking id")
        void shouldThrowWhenNotFound() {
            when(orgApplicationFeeRepository.findByTrackingId("bad-tracking")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orgApplicationFeeService.getByTrackingId("bad-tracking"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── getById ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("Should return fee when found by id")
        void shouldReturnFeeById() {
            when(orgApplicationFeeRepository.findById(FEE_ID)).thenReturn(Optional.of(sampleFee));

            OrganizationApplicationFee result = orgApplicationFeeService.getById(FEE_ID);

            assertThat(result.getId()).isEqualTo(FEE_ID);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found by id")
        void shouldThrowWhenNotFoundById() {
            when(orgApplicationFeeRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orgApplicationFeeService.getById("bad-id"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── initiate ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("initiate")
    class Initiate {

        @Test
        @DisplayName("Should create Stripe payment intent when fee > 0")
        void shouldCreateStripeIntentWhenFeePositive() {
            when(stripeConfig.getCurrency()).thenReturn("usd");

            StripePaymentIntentResponse intentResponse = new StripePaymentIntentResponse();
            intentResponse.setId("pi_test_001");
            intentResponse.setClientSecret("secret_001");
            when(stripePaymentService.createPaymentIntent(any())).thenReturn(intentResponse);
            when(orgApplicationFeeRepository.save(any(OrganizationApplicationFee.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrganizationApplicationFee result = orgApplicationFeeService.initiate(
                    "Test Org", "org@example.com", new BigDecimal("100.00"));

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatusEnum.PENDING);
            assertThat(result.getStripePaymentIntentId()).isEqualTo("pi_test_001");
            assertThat(result.getTrackingId()).isNotNull();
            verify(orgApplicationFeeRepository).save(any());
        }

        @Test
        @DisplayName("Should mark as COMPLETED immediately when fee amount is 0")
        void shouldCompleteWhenFeeIsZero() {
            when(orgApplicationFeeRepository.save(any(OrganizationApplicationFee.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            OrganizationApplicationFee result = orgApplicationFeeService.initiate(
                    "Free Org", "free@example.com", BigDecimal.ZERO);

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatusEnum.COMPLETED);
            verify(stripePaymentService, never()).createPaymentIntent(any());
        }
    }

    // ─── completePayment ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("completePayment")
    class CompletePayment {

        @Test
        @DisplayName("Should return existing completed fee without re-processing")
        void shouldReturnAlreadyCompletedFee() {
            sampleFee.setPaymentStatus(PaymentStatusEnum.COMPLETED);
            when(orgApplicationFeeRepository.findByTrackingId(TRACKING_ID))
                    .thenReturn(Optional.of(sampleFee));

            OrganizationApplicationFee result = orgApplicationFeeService.completePayment(TRACKING_ID, "pm_test");

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatusEnum.COMPLETED);
            verify(stripePaymentService, never()).confirmPaymentIntent(any(), any());
        }

        @Test
        @DisplayName("Should complete payment when Stripe returns succeeded")
        void shouldCompleteOnStripeSuccess() {
            sampleFee.setStripePaymentIntentId("pi_test");
            when(orgApplicationFeeRepository.findByTrackingId(TRACKING_ID))
                    .thenReturn(Optional.of(sampleFee));

            StripePaymentIntentResponse confirmed = new StripePaymentIntentResponse();
            confirmed.setStatus("succeeded");
            when(stripePaymentService.confirmPaymentIntent("pi_test", "pm_test")).thenReturn(confirmed);
            when(orgApplicationFeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OrganizationApplicationFee result = orgApplicationFeeService.completePayment(TRACKING_ID, "pm_test");

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatusEnum.COMPLETED);
        }

        @Test
        @DisplayName("Should mark as FAILED when Stripe returns non-succeeded status")
        void shouldFailWhenStripeFails() {
            sampleFee.setStripePaymentIntentId("pi_test");
            when(orgApplicationFeeRepository.findByTrackingId(TRACKING_ID))
                    .thenReturn(Optional.of(sampleFee));

            StripePaymentIntentResponse confirmed = new StripePaymentIntentResponse();
            confirmed.setStatus("requires_payment_method");
            when(stripePaymentService.confirmPaymentIntent("pi_test", "pm_test")).thenReturn(confirmed);
            when(orgApplicationFeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            OrganizationApplicationFee result = orgApplicationFeeService.completePayment(TRACKING_ID, "pm_test");

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatusEnum.FAILED);
        }
    }

    // ─── isFeePaid ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isFeePaid")
    class IsFeePaid {

        @Test
        @DisplayName("Should return true when fee is completed")
        void shouldReturnTrueWhenCompleted() {
            sampleFee.setPaymentStatus(PaymentStatusEnum.COMPLETED);
            when(orgApplicationFeeRepository.findByTrackingId(TRACKING_ID))
                    .thenReturn(Optional.of(sampleFee));

            assertThat(orgApplicationFeeService.isFeePaid(TRACKING_ID)).isTrue();
        }

        @Test
        @DisplayName("Should return false when fee is pending")
        void shouldReturnFalseWhenPending() {
            when(orgApplicationFeeRepository.findByTrackingId(TRACKING_ID))
                    .thenReturn(Optional.of(sampleFee));

            assertThat(orgApplicationFeeService.isFeePaid(TRACKING_ID)).isFalse();
        }

        @Test
        @DisplayName("Should return false when tracking id not found")
        void shouldReturnFalseWhenNotFound() {
            when(orgApplicationFeeRepository.findByTrackingId("unknown")).thenReturn(Optional.empty());

            assertThat(orgApplicationFeeService.isFeePaid("unknown")).isFalse();
        }
    }
}
