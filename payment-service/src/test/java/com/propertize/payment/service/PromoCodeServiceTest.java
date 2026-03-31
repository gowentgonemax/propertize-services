package com.propertize.payment.service;

import com.propertize.payment.dto.promo.*;
import com.propertize.payment.entity.PromoCode;
import com.propertize.payment.entity.PromoCodeUsage;
import com.propertize.payment.enums.DiscountTypeEnum;
import com.propertize.payment.exception.BadRequestException;
import com.propertize.payment.exception.ResourceNotFoundException;
import com.propertize.payment.repository.PromoCodeRepository;
import com.propertize.payment.repository.PromoCodeUsageRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PromoCodeService Tests")
class PromoCodeServiceTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;
    @Mock
    private PromoCodeUsageRepository promoCodeUsageRepository;

    @InjectMocks
    private PromoCodeService promoCodeService;

    private PromoCode samplePromo;
    private static final String PROMO_ID = "promo-001";
    private static final String ORG_ID = "org-001";

    @BeforeEach
    void setUp() {
        samplePromo = new PromoCode();
        samplePromo.setId(PROMO_ID);
        samplePromo.setCode("SAVE20");
        samplePromo.setOrganizationId(ORG_ID);
        samplePromo.setDiscountType(DiscountTypeEnum.PERCENTAGE);
        samplePromo.setDiscountValue(new BigDecimal("20"));
        samplePromo.setMaxUses(100);
        samplePromo.setCurrentUses(5);
        samplePromo.setActive(true);
        samplePromo.setExpiresAt(LocalDateTime.now().plusDays(30));
    }

    // ──────────────────────── CRUD ────────────────────────

    @Nested
    @DisplayName("CRUD Operations")
    class CrudOperations {

        @Test
        @DisplayName("Should return paginated promo codes for organization")
        void shouldGetPromoCodesByOrganization() {
            when(promoCodeRepository.findByOrganizationId(eq(ORG_ID), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(samplePromo)));

            Page<PromoCode> result = promoCodeService.getPromoCodesByOrganization(ORG_ID, 1, 20);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCode()).isEqualTo("SAVE20");
        }

        @Test
        @DisplayName("Should get promo code by ID")
        void shouldGetPromoCodeById() {
            when(promoCodeRepository.findById(PROMO_ID)).thenReturn(Optional.of(samplePromo));

            PromoCode result = promoCodeService.getPromoCodeById(PROMO_ID);

            assertThat(result.getCode()).isEqualTo("SAVE20");
        }

        @Test
        @DisplayName("Should throw when promo code not found by ID")
        void shouldThrowWhenNotFound() {
            when(promoCodeRepository.findById("nonexistent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> promoCodeService.getPromoCodeById("nonexistent"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should create promo code successfully")
        void shouldCreatePromoCode() {
            PromoCodeRequest request = new PromoCodeRequest();
            request.setCode("new20");
            request.setOrganizationId(ORG_ID);
            request.setDiscountType(DiscountTypeEnum.PERCENTAGE);
            request.setDiscountValue(new BigDecimal("20"));
            request.setActive(true);

            when(promoCodeRepository.findByCodeIgnoreCaseAndOrganizationId("new20", ORG_ID))
                    .thenReturn(Optional.empty());
            when(promoCodeRepository.save(any(PromoCode.class))).thenAnswer(inv -> {
                PromoCode p = inv.getArgument(0);
                p.setId(PROMO_ID);
                return p;
            });

            PromoCode result = promoCodeService.createPromoCode(request);

            assertThat(result.getCode()).isEqualTo("NEW20"); // uppercased
            assertThat(result.getCurrentUses()).isZero();
            verify(promoCodeRepository).save(any());
        }

        @Test
        @DisplayName("Should throw when duplicate promo code for same org")
        void shouldThrowOnDuplicateCode() {
            PromoCodeRequest request = new PromoCodeRequest();
            request.setCode("SAVE20");
            request.setOrganizationId(ORG_ID);
            request.setDiscountType(DiscountTypeEnum.PERCENTAGE);
            request.setDiscountValue(new BigDecimal("20"));

            when(promoCodeRepository.findByCodeIgnoreCaseAndOrganizationId("SAVE20", ORG_ID))
                    .thenReturn(Optional.of(samplePromo));

            assertThatThrownBy(() -> promoCodeService.createPromoCode(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        @DisplayName("Should soft-delete promo code")
        void shouldSoftDeletePromoCode() {
            when(promoCodeRepository.findById(PROMO_ID)).thenReturn(Optional.of(samplePromo));
            when(promoCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            promoCodeService.deletePromoCode(PROMO_ID);

            verify(promoCodeRepository).save(argThat(promo -> !((PromoCode) promo).getActive()));
        }
    }

    // ──────────────────────── Validation ────────────────────────

    @Nested
    @DisplayName("validatePromoCode")
    class ValidatePromoCode {

        @Test
        @DisplayName("Should validate a valid promo code")
        void shouldValidateValidCode() {
            PromoCodeValidateRequest request = new PromoCodeValidateRequest();
            request.setCode("SAVE20");
            request.setOrganizationId(ORG_ID);

            when(promoCodeRepository.findByCodeIgnoreCaseAndOrganizationId("SAVE20", ORG_ID))
                    .thenReturn(Optional.of(samplePromo));

            PromoCodeValidateResponse result = promoCodeService.validatePromoCode(request);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getPromoCodeId()).isEqualTo(PROMO_ID);
            assertThat(result.getDiscountType()).isEqualTo(DiscountTypeEnum.PERCENTAGE);
        }

        @Test
        @DisplayName("Should reject non-existent promo code")
        void shouldRejectNonExistent() {
            PromoCodeValidateRequest request = new PromoCodeValidateRequest();
            request.setCode("FAKE");
            request.setOrganizationId(ORG_ID);

            when(promoCodeRepository.findByCodeIgnoreCaseAndOrganizationId("FAKE", ORG_ID))
                    .thenReturn(Optional.empty());

            PromoCodeValidateResponse result = promoCodeService.validatePromoCode(request);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).contains("not found");
        }

        @Test
        @DisplayName("Should reject inactive promo code")
        void shouldRejectInactive() {
            samplePromo.setActive(false);
            PromoCodeValidateRequest request = new PromoCodeValidateRequest();
            request.setCode("SAVE20");
            request.setOrganizationId(ORG_ID);

            when(promoCodeRepository.findByCodeIgnoreCaseAndOrganizationId("SAVE20", ORG_ID))
                    .thenReturn(Optional.of(samplePromo));

            PromoCodeValidateResponse result = promoCodeService.validatePromoCode(request);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).contains("not active");
        }

        @Test
        @DisplayName("Should reject expired promo code")
        void shouldRejectExpired() {
            samplePromo.setExpiresAt(LocalDateTime.now().minusDays(1));
            PromoCodeValidateRequest request = new PromoCodeValidateRequest();
            request.setCode("SAVE20");
            request.setOrganizationId(ORG_ID);

            when(promoCodeRepository.findByCodeIgnoreCaseAndOrganizationId("SAVE20", ORG_ID))
                    .thenReturn(Optional.of(samplePromo));

            PromoCodeValidateResponse result = promoCodeService.validatePromoCode(request);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).contains("expired");
        }

        @Test
        @DisplayName("Should reject promo code that exceeded max uses")
        void shouldRejectMaxUsesExceeded() {
            samplePromo.setMaxUses(5);
            samplePromo.setCurrentUses(5);
            PromoCodeValidateRequest request = new PromoCodeValidateRequest();
            request.setCode("SAVE20");
            request.setOrganizationId(ORG_ID);

            when(promoCodeRepository.findByCodeIgnoreCaseAndOrganizationId("SAVE20", ORG_ID))
                    .thenReturn(Optional.of(samplePromo));

            PromoCodeValidateResponse result = promoCodeService.validatePromoCode(request);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).contains("maximum usage limit");
        }

        @Test
        @DisplayName("Should reject promo code already used for application")
        void shouldRejectAlreadyUsedForApplication() {
            PromoCodeValidateRequest request = new PromoCodeValidateRequest();
            request.setCode("SAVE20");
            request.setOrganizationId(ORG_ID);
            request.setApplicationId("app-001");

            when(promoCodeRepository.findByCodeIgnoreCaseAndOrganizationId("SAVE20", ORG_ID))
                    .thenReturn(Optional.of(samplePromo));
            when(promoCodeUsageRepository.existsByPromoCodeIdAndApplicationId(PROMO_ID, "app-001"))
                    .thenReturn(true);

            PromoCodeValidateResponse result = promoCodeService.validatePromoCode(request);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getMessage()).contains("already been applied");
        }
    }

    // ──────────────────────── Discount Calculation ────────────────────────

    @Nested
    @DisplayName("calculateDiscount")
    class CalculateDiscount {

        @Test
        @DisplayName("Should calculate percentage discount")
        void shouldCalculatePercentageDiscount() {
            when(promoCodeRepository.findById(PROMO_ID)).thenReturn(Optional.of(samplePromo));

            BigDecimal discount = promoCodeService.calculateDiscount(PROMO_ID, new BigDecimal("500.00"));

            assertThat(discount).isEqualByComparingTo("100.00"); // 20% of 500
        }

        @Test
        @DisplayName("Should calculate fixed discount capped at base amount")
        void shouldCalculateFixedDiscount() {
            samplePromo.setDiscountType(DiscountTypeEnum.FIXED);
            samplePromo.setDiscountValue(new BigDecimal("50.00"));
            when(promoCodeRepository.findById(PROMO_ID)).thenReturn(Optional.of(samplePromo));

            BigDecimal discount = promoCodeService.calculateDiscount(PROMO_ID, new BigDecimal("500.00"));

            assertThat(discount).isEqualByComparingTo("50.00");
        }

        @Test
        @DisplayName("Should cap fixed discount at base amount when larger")
        void shouldCapFixedDiscount() {
            samplePromo.setDiscountType(DiscountTypeEnum.FIXED);
            samplePromo.setDiscountValue(new BigDecimal("1000.00"));
            when(promoCodeRepository.findById(PROMO_ID)).thenReturn(Optional.of(samplePromo));

            BigDecimal discount = promoCodeService.calculateDiscount(PROMO_ID, new BigDecimal("200.00"));

            assertThat(discount).isEqualByComparingTo("200.00"); // capped
        }

        @Test
        @DisplayName("Should waive entire amount")
        void shouldWaiveEntireAmount() {
            samplePromo.setDiscountType(DiscountTypeEnum.WAIVE);
            when(promoCodeRepository.findById(PROMO_ID)).thenReturn(Optional.of(samplePromo));

            BigDecimal discount = promoCodeService.calculateDiscount(PROMO_ID, new BigDecimal("750.00"));

            assertThat(discount).isEqualByComparingTo("750.00");
        }
    }

    // ──────────────────────── recordUsage ────────────────────────

    @Test
    @DisplayName("Should record promo code usage and increment counter")
    void shouldRecordUsage() {
        when(promoCodeRepository.findById(PROMO_ID)).thenReturn(Optional.of(samplePromo));
        when(promoCodeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(promoCodeUsageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        promoCodeService.recordUsage(PROMO_ID, ORG_ID, "app-001", "test@example.com", new BigDecimal("100.00"));

        verify(promoCodeRepository).save(argThat(promo -> ((PromoCode) promo).getCurrentUses() == 6));
        verify(promoCodeUsageRepository).save(any(PromoCodeUsage.class));
    }

    // ──────────────────────── Validation Helpers ────────────────────────

    @Nested
    @DisplayName("Discount Value Validation")
    class DiscountValueValidation {

        @Test
        @DisplayName("Should reject percentage discount over 100%")
        void shouldRejectPercentageOver100() {
            PromoCodeRequest request = new PromoCodeRequest();
            request.setCode("BAD");
            request.setOrganizationId(ORG_ID);
            request.setDiscountType(DiscountTypeEnum.PERCENTAGE);
            request.setDiscountValue(new BigDecimal("101"));

            when(promoCodeRepository.findByCodeIgnoreCaseAndOrganizationId("BAD", ORG_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> promoCodeService.createPromoCode(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("100%");
        }

        @Test
        @DisplayName("Should reject zero or negative discount value")
        void shouldRejectZeroDiscount() {
            PromoCodeRequest request = new PromoCodeRequest();
            request.setCode("BAD");
            request.setOrganizationId(ORG_ID);
            request.setDiscountType(DiscountTypeEnum.FIXED);
            request.setDiscountValue(BigDecimal.ZERO);

            when(promoCodeRepository.findByCodeIgnoreCaseAndOrganizationId("BAD", ORG_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> promoCodeService.createPromoCode(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("greater than 0");
        }
    }
}
