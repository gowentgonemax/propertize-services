package com.propertize.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.propertize.payment.dto.promo.PromoCodeRequest;
import com.propertize.payment.dto.promo.PromoCodeValidateRequest;
import com.propertize.payment.dto.promo.PromoCodeValidateResponse;
import com.propertize.payment.entity.PromoCode;
import com.propertize.payment.enums.DiscountTypeEnum;
import com.propertize.commons.exception.ResourceNotFoundException;
import com.propertize.payment.service.PromoCodeService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PromoCodeController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@DisplayName("PromoCodeController Tests")
class PromoCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PromoCodeService promoCodeService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private PromoCode samplePromo;
    private static final String ORG_ID = "org-001";
    private static final String PROMO_ID = "promo-001";

    @BeforeEach
    void setUp() {
        samplePromo = new PromoCode();
        samplePromo.setId(PROMO_ID);
        samplePromo.setCode("SAVE20");
        samplePromo.setOrganizationId(ORG_ID);
        samplePromo.setDiscountType(DiscountTypeEnum.PERCENTAGE);
        samplePromo.setDiscountValue(new BigDecimal("20"));
        samplePromo.setActive(true);
    }

    @Test
    @DisplayName("GET /api/v1/promo-codes — should return paginated promo codes")
    void shouldGetPromoCodesByOrganization() throws Exception {
        Page<PromoCode> page = new PageImpl<>(List.of(samplePromo));
        when(promoCodeService.getPromoCodesByOrganization(ORG_ID, 1, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/promo-codes")
                .param("organizationId", ORG_ID)
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/promo-codes/{id} — should return promo code by id")
    void shouldGetPromoCodeById() throws Exception {
        when(promoCodeService.getPromoCodeById(PROMO_ID)).thenReturn(samplePromo);

        mockMvc.perform(get("/api/v1/promo-codes/{id}", PROMO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("SAVE20"));
    }

    @Test
    @DisplayName("GET /api/v1/promo-codes/{id} — should return 404 when not found")
    void shouldReturn404WhenPromoCodeNotFound() throws Exception {
        when(promoCodeService.getPromoCodeById("bad-id"))
                .thenThrow(new ResourceNotFoundException("PromoCode", "id", "bad-id"));

        mockMvc.perform(get("/api/v1/promo-codes/bad-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/promo-codes — should create promo code")
    void shouldCreatePromoCode() throws Exception {
        PromoCodeRequest request = new PromoCodeRequest();
        request.setCode("NEW30");
        request.setOrganizationId(ORG_ID);
        request.setDiscountType(DiscountTypeEnum.PERCENTAGE);
        request.setDiscountValue(new BigDecimal("30"));

        when(promoCodeService.createPromoCode(any(PromoCodeRequest.class))).thenReturn(samplePromo);

        mockMvc.perform(post("/api/v1/promo-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/promo-codes — should reject missing required fields")
    void shouldRejectInvalidCreateRequest() throws Exception {
        PromoCodeRequest request = new PromoCodeRequest();
        // Missing code, organizationId, discountType, discountValue

        mockMvc.perform(post("/api/v1/promo-codes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/v1/promo-codes/{id} — should update promo code")
    void shouldUpdatePromoCode() throws Exception {
        PromoCodeRequest request = new PromoCodeRequest();
        request.setCode("UPDATED30");
        request.setOrganizationId(ORG_ID);
        request.setDiscountType(DiscountTypeEnum.PERCENTAGE);
        request.setDiscountValue(new BigDecimal("30"));

        samplePromo.setCode("UPDATED30");
        when(promoCodeService.updatePromoCode(eq(PROMO_ID), any(PromoCodeRequest.class))).thenReturn(samplePromo);

        mockMvc.perform(put("/api/v1/promo-codes/{id}", PROMO_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/v1/promo-codes/{id} — should delete promo code")
    void shouldDeletePromoCode() throws Exception {
        doNothing().when(promoCodeService).deletePromoCode(PROMO_ID);

        mockMvc.perform(delete("/api/v1/promo-codes/{id}", PROMO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/promo-codes/validate — should validate promo code")
    void shouldValidatePromoCode() throws Exception {
        PromoCodeValidateRequest request = new PromoCodeValidateRequest();
        request.setCode("SAVE20");
        request.setOrganizationId(ORG_ID);

        PromoCodeValidateResponse response = new PromoCodeValidateResponse();
        response.setValid(true);
        response.setPromoCodeId(PROMO_ID);
        when(promoCodeService.validatePromoCode(any(PromoCodeValidateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/promo-codes/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
