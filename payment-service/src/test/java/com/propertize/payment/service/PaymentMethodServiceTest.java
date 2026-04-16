package com.propertize.payment.service;

import com.propertize.payment.dto.payment.request.CreateACHPaymentMethodRequest;
import com.propertize.payment.dto.payment.request.CreateStripePaymentMethodRequest;
import com.propertize.payment.entity.PaymentMethod;
import com.propertize.commons.enums.payment.BankAccountTypeEnum;
import com.propertize.commons.enums.payment.PaymentMethodEnum;
import com.propertize.commons.exception.BadRequestException;
import com.propertize.commons.exception.ResourceNotFoundException;
import com.propertize.payment.repository.PaymentMethodRepository;
import com.propertize.payment.service.payment.StripePaymentService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentMethodService Tests")
class PaymentMethodServiceTest {

    @Mock
    private PaymentMethodRepository paymentMethodRepository;
    @Mock
    private StripePaymentService stripePaymentService;

    @InjectMocks
    private PaymentMethodService paymentMethodService;

    private static final String PM_ID = "pm-001";
    private static final String TENANT_ID = "tenant-001";
    private static final String ORG_ID = "org-001";

    private PaymentMethod samplePm;

    @BeforeEach
    void setUp() {
        samplePm = new PaymentMethod();
        samplePm.setId(PM_ID);
        samplePm.setTenantId(TENANT_ID);
        samplePm.setOrganizationId(ORG_ID);
        samplePm.setMethodType(PaymentMethodEnum.CREDIT_CARD);
        samplePm.setStripePaymentMethodId("stripe_pm_001");
        samplePm.setLastFour("4242");
        samplePm.setIsActive(true);
        samplePm.setIsDefault(false);
    }

    // ─── getPaymentMethodsByTenant ───────────────────────────────────────────

    @Test
    @DisplayName("Should return active payment methods for tenant")
    void shouldReturnActivePaymentMethodsForTenant() {
        when(paymentMethodRepository.findByTenantIdAndIsActiveTrue(TENANT_ID))
                .thenReturn(List.of(samplePm));

        List<PaymentMethod> results = paymentMethodService.getPaymentMethodsByTenant(TENANT_ID);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTenantId()).isEqualTo(TENANT_ID);
    }

    // ─── getPaymentMethodById ────────────────────────────────────────────────

    @Test
    @DisplayName("Should return payment method by id")
    void shouldReturnPaymentMethodById() {
        when(paymentMethodRepository.findById(PM_ID)).thenReturn(Optional.of(samplePm));

        PaymentMethod result = paymentMethodService.getPaymentMethodById(PM_ID);

        assertThat(result.getId()).isEqualTo(PM_ID);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when payment method not found")
    void shouldThrowWhenNotFound() {
        when(paymentMethodRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentMethodService.getPaymentMethodById("bad-id"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── addCardPaymentMethod ────────────────────────────────────────────────

    @Nested
    @DisplayName("addCardPaymentMethod")
    class AddCardPaymentMethod {

        private CreateStripePaymentMethodRequest buildRequest() {
            CreateStripePaymentMethodRequest req = new CreateStripePaymentMethodRequest();
            req.setOrganizationId(ORG_ID);
            req.setTenantId(TENANT_ID);
            req.setStripePaymentMethodId("pm_new_001");
            req.setStripeCustomerId("cus_001");
            req.setBrand("VISA");
            req.setLastFour("4242");
            req.setExpMonth(12);
            req.setExpYear(2027);
            req.setFingerprint("fp_unique_001");
            req.setCardholderName("John Doe");
            return req;
        }

        @Test
        @DisplayName("Should add card and mark as default when no existing active methods")
        void shouldAddCardAsDefaultWhenFirstCard() {
            CreateStripePaymentMethodRequest req = buildRequest();

            when(paymentMethodRepository.findByFingerprintAndOrganizationId("fp_unique_001", ORG_ID))
                    .thenReturn(Optional.empty());
            when(paymentMethodRepository.findByTenantIdAndIsActiveTrue(TENANT_ID)).thenReturn(List.of());
            when(paymentMethodRepository.save(any(PaymentMethod.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PaymentMethod result = paymentMethodService.addCardPaymentMethod(req);

            assertThat(result.getMethodType()).isEqualTo(PaymentMethodEnum.CREDIT_CARD);
            assertThat(result.getIsDefault()).isTrue();
            assertThat(result.getIsActive()).isTrue();
            verify(stripePaymentService).attachPaymentMethod("pm_new_001", "cus_001");
        }

        @Test
        @DisplayName("Should not make card default when other active methods exist")
        void shouldNotBeDefaultWhenOtherMethodsExist() {
            CreateStripePaymentMethodRequest req = buildRequest();

            when(paymentMethodRepository.findByFingerprintAndOrganizationId("fp_unique_001", ORG_ID))
                    .thenReturn(Optional.empty());
            when(paymentMethodRepository.findByTenantIdAndIsActiveTrue(TENANT_ID))
                    .thenReturn(List.of(samplePm));
            when(paymentMethodRepository.save(any(PaymentMethod.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PaymentMethod result = paymentMethodService.addCardPaymentMethod(req);

            assertThat(result.getIsDefault()).isFalse();
        }

        @Test
        @DisplayName("Should throw BadRequestException when duplicate fingerprint found")
        void shouldThrowOnDuplicateFingerprint() {
            CreateStripePaymentMethodRequest req = buildRequest();

            when(paymentMethodRepository.findByFingerprintAndOrganizationId("fp_unique_001", ORG_ID))
                    .thenReturn(Optional.of(samplePm));

            assertThatThrownBy(() -> paymentMethodService.addCardPaymentMethod(req))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already on file");
        }
    }

    // ─── addACHPaymentMethod ─────────────────────────────────────────────────

    @Nested
    @DisplayName("addACHPaymentMethod")
    class AddACHPaymentMethod {

        @Test
        @DisplayName("Should add ACH method and mark as default when no existing methods")
        void shouldAddACHAsDefaultWhenFirst() {
            CreateACHPaymentMethodRequest req = new CreateACHPaymentMethodRequest();
            req.setOrganizationId(ORG_ID);
            req.setTenantId(TENANT_ID);
            req.setStripePaymentMethodId("pm_ach_001");
            req.setStripeCustomerId("cus_001");
            req.setBankName("Chase");
            req.setAccountType(BankAccountTypeEnum.CHECKING);
            req.setLastFour("6789");

            when(paymentMethodRepository.findByTenantIdAndIsActiveTrue(TENANT_ID)).thenReturn(List.of());
            when(paymentMethodRepository.save(any(PaymentMethod.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            PaymentMethod result = paymentMethodService.addACHPaymentMethod(req);

            assertThat(result.getMethodType()).isEqualTo(PaymentMethodEnum.ACH);
            assertThat(result.getBankName()).isEqualTo("Chase");
            assertThat(result.getIsDefault()).isTrue();
            verify(stripePaymentService).attachPaymentMethod("pm_ach_001", "cus_001");
        }
    }

    // ─── setDefaultPaymentMethod ─────────────────────────────────────────────

    @Nested
    @DisplayName("setDefaultPaymentMethod")
    class SetDefault {

        @Test
        @DisplayName("Should set new default and unset previous default")
        void shouldSetNewDefault() {
            PaymentMethod currentDefault = new PaymentMethod();
            currentDefault.setId("pm-old-default");
            currentDefault.setIsDefault(true);
            currentDefault.setIsActive(true);

            PaymentMethod newDefault = new PaymentMethod();
            newDefault.setId("pm-new-default");
            newDefault.setIsDefault(false);
            newDefault.setIsActive(true);

            when(paymentMethodRepository.findById("pm-new-default")).thenReturn(Optional.of(newDefault));
            when(paymentMethodRepository.findByTenantIdAndIsActiveTrue(TENANT_ID))
                    .thenReturn(List.of(currentDefault, newDefault));
            when(paymentMethodRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            paymentMethodService.setDefaultPaymentMethod("pm-new-default", TENANT_ID);

            assertThat(currentDefault.getIsDefault()).isFalse();
            assertThat(newDefault.getIsDefault()).isTrue();
        }
    }

    // ─── deletePaymentMethod ─────────────────────────────────────────────────

    @Nested
    @DisplayName("deletePaymentMethod")
    class DeletePaymentMethod {

        @Test
        @DisplayName("Should soft-delete and detach from Stripe")
        void shouldSoftDeleteAndDetachFromStripe() {
            when(paymentMethodRepository.findById(PM_ID)).thenReturn(Optional.of(samplePm));
            when(paymentMethodRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            paymentMethodService.deletePaymentMethod(PM_ID);

            assertThat(samplePm.getIsActive()).isFalse();
            verify(stripePaymentService).detachPaymentMethod("stripe_pm_001");
            verify(paymentMethodRepository).save(samplePm);
        }

        @Test
        @DisplayName("Should soft-delete without Stripe call when no stripe id")
        void shouldSoftDeleteWithoutStripeCall() {
            samplePm.setStripePaymentMethodId(null);
            when(paymentMethodRepository.findById(PM_ID)).thenReturn(Optional.of(samplePm));
            when(paymentMethodRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            paymentMethodService.deletePaymentMethod(PM_ID);

            assertThat(samplePm.getIsActive()).isFalse();
            verify(stripePaymentService, never()).detachPaymentMethod(any());
        }
    }
}
