package com.propertize.payment.service;

import com.propertize.payment.config.StripeConfig;
import com.propertize.payment.dto.payment.request.ApplicationFeeProcessPaymentRequest;
import com.propertize.payment.dto.payment.request.ApplicationFeeRequest;
import com.propertize.payment.dto.payment.response.StripePaymentIntentResponse;
import com.propertize.payment.dto.promo.PromoCodeValidateRequest;
import com.propertize.payment.dto.promo.PromoCodeValidateResponse;
import com.propertize.payment.entity.ApplicationFee;
import com.propertize.commons.enums.payment.PaymentStatusEnum;
import com.propertize.commons.exception.BadRequestException;
import com.propertize.commons.exception.ResourceNotFoundException;
import com.propertize.payment.repository.ApplicationFeeRepository;
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
@DisplayName("ApplicationFeeService Tests")
class ApplicationFeeServiceTest {

    @Mock
    private ApplicationFeeRepository applicationFeeRepository;
    @Mock
    private StripePaymentService stripePaymentService;
    @Mock
    private PromoCodeService promoCodeService;
    @Mock
    private StripeConfig stripeConfig;

    @InjectMocks
    private ApplicationFeeService applicationFeeService;

    private static final String FEE_ID = "fee-001";
    private static final String ORG_ID = "org-001";
    private static final String APP_ID = "app-001";
    private static final String EMAIL = "tenant@example.com";

    private ApplicationFee sampleFee;

    @BeforeEach
    void setUp() {
        sampleFee = new ApplicationFee();
        sampleFee.setId(FEE_ID);
        sampleFee.setOrganizationId(ORG_ID);
        sampleFee.setRentalApplicationId(APP_ID);
        sampleFee.setApplicantEmail(EMAIL);
        sampleFee.setFeeAmount(new BigDecimal("50.00"));
        sampleFee.setPaymentStatus(PaymentStatusEnum.PENDING);
    }

    // ─── getApplicationFeeById ───────────────────────────────────────────────

    @Nested
    @DisplayName("getApplicationFeeById")
    class GetById {

        @Test
        @DisplayName("Should return fee when found")
        void shouldReturnFeeWhenFound() {
            when(applicationFeeRepository.findById(FEE_ID)).thenReturn(Optional.of(sampleFee));

            ApplicationFee result = applicationFeeService.getApplicationFeeById(FEE_ID);

            assertThat(result.getId()).isEqualTo(FEE_ID);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(applicationFeeRepository.findById("bad-id")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationFeeService.getApplicationFeeById("bad-id"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── getApplicationFeeByRentalApplicationId ──────────────────────────────

    @Nested
    @DisplayName("getApplicationFeeByRentalApplicationId")
    class GetByRentalApplication {

        @Test
        @DisplayName("Should return fee when found by rental application id")
        void shouldReturnFeeByRentalAppId() {
            when(applicationFeeRepository.findByRentalApplicationId(APP_ID))
                    .thenReturn(Optional.of(sampleFee));

            ApplicationFee result = applicationFeeService.getApplicationFeeByRentalApplicationId(APP_ID);

            assertThat(result.getRentalApplicationId()).isEqualTo(APP_ID);
        }

        @Test
        @DisplayName("Should throw ResourceNotFoundException when not found by rental application id")
        void shouldThrowWhenNotFoundByRentalAppId() {
            when(applicationFeeRepository.findByRentalApplicationId("unknown"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> applicationFeeService.getApplicationFeeByRentalApplicationId("unknown"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─── createApplicationFee ────────────────────────────────────────────────

    @Nested
    @DisplayName("createApplicationFee")
    class CreateApplicationFee {

        private ApplicationFeeRequest buildRequest() {
            ApplicationFeeRequest req = new ApplicationFeeRequest();
            req.setOrganizationId(ORG_ID);
            req.setRentalApplicationId(APP_ID);
            req.setApplicantEmail(EMAIL);
            req.setFeeAmount(new BigDecimal("50.00"));
            return req;
        }

        @Test
        @DisplayName("Should create fee with Stripe payment intent when amount > 0")
        void shouldCreateFeeWithStripeIntent() {
            ApplicationFeeRequest req = buildRequest();

            when(applicationFeeRepository.findByRentalApplicationId(APP_ID)).thenReturn(Optional.empty());
            when(stripeConfig.getCurrency()).thenReturn("usd");

            StripePaymentIntentResponse intentResponse = new StripePaymentIntentResponse();
            intentResponse.setId("pi_test_001");
            intentResponse.setClientSecret("secret_001");
            when(stripePaymentService.createPaymentIntent(any())).thenReturn(intentResponse);

            when(applicationFeeRepository.save(any(ApplicationFee.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ApplicationFee result = applicationFeeService.createApplicationFee(req);

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatusEnum.PENDING);
            assertThat(result.getStripePaymentIntentId()).isEqualTo("pi_test_001");
            verify(stripePaymentService).createPaymentIntent(any());
        }

        @Test
        @DisplayName("Should throw BadRequestException when fee already exists for application")
        void shouldThrowOnDuplicateFee() {
            ApplicationFeeRequest req = buildRequest();

            when(applicationFeeRepository.findByRentalApplicationId(APP_ID))
                    .thenReturn(Optional.of(sampleFee));

            assertThatThrownBy(() -> applicationFeeService.createApplicationFee(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should complete fee immediately when promo code waives full amount")
        void shouldCompleteWhenPromoWaivesFullAmount() {
            ApplicationFeeRequest req = buildRequest();
            req.setPromoCode("FREEFEE");

            when(applicationFeeRepository.findByRentalApplicationId(APP_ID)).thenReturn(Optional.empty());

            PromoCodeValidateResponse validResp = new PromoCodeValidateResponse();
            validResp.setValid(true);
            validResp.setPromoCodeId("promo-001");
            when(promoCodeService.validatePromoCode(any(PromoCodeValidateRequest.class))).thenReturn(validResp);

            // Discount equals full fee amount
            when(promoCodeService.calculateDiscount(eq("promo-001"), any(BigDecimal.class)))
                    .thenReturn(new BigDecimal("50.00"));

            when(applicationFeeRepository.save(any(ApplicationFee.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ApplicationFee result = applicationFeeService.createApplicationFee(req);

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatusEnum.COMPLETED);
            verify(stripePaymentService, never()).createPaymentIntent(any());
        }

        @Test
        @DisplayName("Should apply partial promo discount and still create Stripe intent")
        void shouldApplyPartialDiscount() {
            ApplicationFeeRequest req = buildRequest();
            req.setPromoCode("HALF");

            when(applicationFeeRepository.findByRentalApplicationId(APP_ID)).thenReturn(Optional.empty());

            PromoCodeValidateResponse validResp = new PromoCodeValidateResponse();
            validResp.setValid(true);
            validResp.setPromoCodeId("promo-002");
            when(promoCodeService.validatePromoCode(any(PromoCodeValidateRequest.class))).thenReturn(validResp);
            when(promoCodeService.calculateDiscount(eq("promo-002"), any(BigDecimal.class)))
                    .thenReturn(new BigDecimal("25.00"));
            when(stripeConfig.getCurrency()).thenReturn("usd");

            StripePaymentIntentResponse intentResponse = new StripePaymentIntentResponse();
            intentResponse.setId("pi_002");
            intentResponse.setClientSecret("secret_002");
            when(stripePaymentService.createPaymentIntent(any())).thenReturn(intentResponse);
            when(applicationFeeRepository.save(any(ApplicationFee.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ApplicationFee result = applicationFeeService.createApplicationFee(req);

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatusEnum.PENDING);
            assertThat(result.getFinalAmount()).isEqualByComparingTo("25.00");
        }
    }

    // ─── processPayment ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("processPayment")
    class ProcessPayment {

        @Test
        @DisplayName("Should complete payment when Stripe confirms with 'succeeded'")
        void shouldCompleteOnSuccess() {
            sampleFee.setStripePaymentIntentId("pi_test");
            when(applicationFeeRepository.findById(FEE_ID)).thenReturn(Optional.of(sampleFee));

            StripePaymentIntentResponse confirmed = new StripePaymentIntentResponse();
            confirmed.setStatus("succeeded");
            when(stripePaymentService.confirmPaymentIntent("pi_test", "pm_test"))
                    .thenReturn(confirmed);
            when(applicationFeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ApplicationFeeProcessPaymentRequest req = new ApplicationFeeProcessPaymentRequest();
            req.setStripePaymentMethodId("pm_test");

            ApplicationFee result = applicationFeeService.processPayment(FEE_ID, req);

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatusEnum.COMPLETED);
        }

        @Test
        @DisplayName("Should fail payment when Stripe returns non-succeeded status")
        void shouldFailOnStripeFailure() {
            sampleFee.setStripePaymentIntentId("pi_test");
            when(applicationFeeRepository.findById(FEE_ID)).thenReturn(Optional.of(sampleFee));

            StripePaymentIntentResponse confirmed = new StripePaymentIntentResponse();
            confirmed.setStatus("requires_action");
            when(stripePaymentService.confirmPaymentIntent("pi_test", "pm_test"))
                    .thenReturn(confirmed);
            when(applicationFeeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ApplicationFeeProcessPaymentRequest req = new ApplicationFeeProcessPaymentRequest();
            req.setStripePaymentMethodId("pm_test");

            ApplicationFee result = applicationFeeService.processPayment(FEE_ID, req);

            assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatusEnum.FAILED);
        }

        @Test
        @DisplayName("Should throw BadRequestException when fee is already paid")
        void shouldThrowWhenAlreadyPaid() {
            sampleFee.setPaymentStatus(PaymentStatusEnum.COMPLETED);
            when(applicationFeeRepository.findById(FEE_ID)).thenReturn(Optional.of(sampleFee));

            ApplicationFeeProcessPaymentRequest req = new ApplicationFeeProcessPaymentRequest();
            req.setStripePaymentMethodId("pm_test");

            assertThatThrownBy(() -> applicationFeeService.processPayment(FEE_ID, req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already paid");
        }
    }
}
