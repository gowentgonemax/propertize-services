package com.propertize.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertize.payment.dto.payment.request.CreateACHPaymentMethodRequest;
import com.propertize.payment.dto.payment.request.CreateStripePaymentMethodRequest;
import com.propertize.payment.entity.PaymentMethod;
import com.propertize.commons.enums.payment.BankAccountTypeEnum;
import com.propertize.commons.enums.payment.PaymentMethodEnum;
import com.propertize.commons.exception.ResourceNotFoundException;
import com.propertize.payment.service.PaymentMethodService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PaymentMethodController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@DisplayName("PaymentMethodController Tests")
class PaymentMethodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentMethodService paymentMethodService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private PaymentMethod samplePm;
    private static final String PM_ID = "pm-001";
    private static final String TENANT_ID = "tenant-001";

    @BeforeEach
    void setUp() {
        samplePm = new PaymentMethod();
        samplePm.setId(PM_ID);
        samplePm.setTenantId(TENANT_ID);
        samplePm.setMethodType(PaymentMethodEnum.CREDIT_CARD);
        samplePm.setLastFour("4242");
        samplePm.setIsActive(true);
        samplePm.setIsDefault(true);
    }

    @Test
    @DisplayName("GET /api/v1/payment-methods/tenant/{tenantId} — should return tenant payment methods")
    void shouldGetPaymentMethodsByTenant() throws Exception {
        when(paymentMethodService.getPaymentMethodsByTenant(TENANT_ID)).thenReturn(List.of(samplePm));

        mockMvc.perform(get("/api/v1/payment-methods/tenant/{tenantId}", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/payment-methods/{id} — should return payment method by id")
    void shouldGetPaymentMethodById() throws Exception {
        when(paymentMethodService.getPaymentMethodById(PM_ID)).thenReturn(samplePm);

        mockMvc.perform(get("/api/v1/payment-methods/{id}", PM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(PM_ID));
    }

    @Test
    @DisplayName("GET /api/v1/payment-methods/{id} — should return 404 when not found")
    void shouldReturn404WhenNotFound() throws Exception {
        when(paymentMethodService.getPaymentMethodById("bad-id"))
                .thenThrow(new ResourceNotFoundException("PaymentMethod", "id", "bad-id"));

        mockMvc.perform(get("/api/v1/payment-methods/bad-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/payment-methods/card — should add card payment method")
    void shouldAddCardPaymentMethod() throws Exception {
        CreateStripePaymentMethodRequest request = new CreateStripePaymentMethodRequest();
        request.setOrganizationId("org-001");
        request.setTenantId(TENANT_ID);
        request.setStripePaymentMethodId("pm_test_001");
        request.setStripeCustomerId("cus_001");
        request.setBrand("VISA");
        request.setLastFour("4242");
        request.setExpMonth(12);
        request.setExpYear(2027);

        when(paymentMethodService.addCardPaymentMethod(any(CreateStripePaymentMethodRequest.class)))
                .thenReturn(samplePm);

        mockMvc.perform(post("/api/v1/payment-methods/card")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/payment-methods/ach — should add ACH payment method")
    void shouldAddACHPaymentMethod() throws Exception {
        CreateACHPaymentMethodRequest request = new CreateACHPaymentMethodRequest();
        request.setOrganizationId("org-001");
        request.setTenantId(TENANT_ID);
        request.setStripePaymentMethodId("pm_ach_001");
        request.setStripeCustomerId("cus_001");
        request.setBankName("Chase");
        request.setAccountType(BankAccountTypeEnum.CHECKING);
        request.setLastFour("6789");

        PaymentMethod achPm = new PaymentMethod();
        achPm.setId("pm-ach-001");
        achPm.setMethodType(PaymentMethodEnum.ACH);
        when(paymentMethodService.addACHPaymentMethod(any(CreateACHPaymentMethodRequest.class))).thenReturn(achPm);

        mockMvc.perform(post("/api/v1/payment-methods/ach")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/payment-methods/{id}/default — should set default payment method")
    void shouldSetDefaultPaymentMethod() throws Exception {
        doNothing().when(paymentMethodService).setDefaultPaymentMethod(PM_ID, TENANT_ID);

        mockMvc.perform(post("/api/v1/payment-methods/{id}/default", PM_ID)
                .param("tenantId", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("DELETE /api/v1/payment-methods/{id} — should delete payment method")
    void shouldDeletePaymentMethod() throws Exception {
        doNothing().when(paymentMethodService).deletePaymentMethod(PM_ID);

        mockMvc.perform(delete("/api/v1/payment-methods/{id}", PM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
