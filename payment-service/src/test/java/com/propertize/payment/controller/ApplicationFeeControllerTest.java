package com.propertize.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.propertize.payment.dto.payment.request.ApplicationFeeProcessPaymentRequest;
import com.propertize.payment.dto.payment.request.ApplicationFeeRequest;
import com.propertize.payment.entity.ApplicationFee;
import com.propertize.payment.enums.PaymentStatusEnum;
import com.propertize.commons.exception.BadRequestException;
import com.propertize.payment.exception.ResourceNotFoundException;
import com.propertize.payment.service.ApplicationFeeService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ApplicationFeeController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@DisplayName("ApplicationFeeController Tests")
class ApplicationFeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApplicationFeeService applicationFeeService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private ApplicationFee sampleFee;
    private static final String FEE_ID = "fee-001";
    private static final String APP_ID = "app-001";

    @BeforeEach
    void setUp() {
        sampleFee = new ApplicationFee();
        sampleFee.setId(FEE_ID);
        sampleFee.setOrganizationId("org-001");
        sampleFee.setRentalApplicationId(APP_ID);
        sampleFee.setApplicantEmail("tenant@example.com");
        sampleFee.setFeeAmount(new BigDecimal("50.00"));
        sampleFee.setPaymentStatus(PaymentStatusEnum.PENDING);
    }

    @Test
    @DisplayName("GET /api/v1/application-fees/{id} — should return application fee by id")
    void shouldGetApplicationFeeById() throws Exception {
        when(applicationFeeService.getApplicationFeeById(FEE_ID)).thenReturn(sampleFee);

        mockMvc.perform(get("/api/v1/application-fees/{id}", FEE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(FEE_ID));
    }

    @Test
    @DisplayName("GET /api/v1/application-fees/{id} — should return 404 when not found")
    void shouldReturn404WhenFeeNotFound() throws Exception {
        when(applicationFeeService.getApplicationFeeById("bad-id"))
                .thenThrow(new ResourceNotFoundException("ApplicationFee", "id", "bad-id"));

        mockMvc.perform(get("/api/v1/application-fees/bad-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/application-fees/by-application/{rentalApplicationId} — should return by application")
    void shouldGetFeeByRentalApplicationId() throws Exception {
        when(applicationFeeService.getApplicationFeeByRentalApplicationId(APP_ID)).thenReturn(sampleFee);

        mockMvc.perform(get("/api/v1/application-fees/by-application/{rentalApplicationId}", APP_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rentalApplicationId").value(APP_ID));
    }

    @Test
    @DisplayName("GET /api/v1/application-fees/by-application/{rentalApplicationId} — should return 404 when not found")
    void shouldReturn404WhenApplicationFeeNotFound() throws Exception {
        when(applicationFeeService.getApplicationFeeByRentalApplicationId("bad-app"))
                .thenThrow(new ResourceNotFoundException("ApplicationFee", "rentalApplicationId", "bad-app"));

        mockMvc.perform(get("/api/v1/application-fees/by-application/bad-app"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/application-fees — should create application fee")
    void shouldCreateApplicationFee() throws Exception {
        ApplicationFeeRequest request = new ApplicationFeeRequest();
        request.setOrganizationId("org-001");
        request.setRentalApplicationId(APP_ID);
        request.setApplicantEmail("tenant@example.com");
        request.setFeeAmount(new BigDecimal("50.00"));

        when(applicationFeeService.createApplicationFee(any(ApplicationFeeRequest.class))).thenReturn(sampleFee);

        mockMvc.perform(post("/api/v1/application-fees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/application-fees — should return 400 on duplicate")
    void shouldReturn400OnDuplicate() throws Exception {
        ApplicationFeeRequest request = new ApplicationFeeRequest();
        request.setOrganizationId("org-001");
        request.setRentalApplicationId(APP_ID);
        request.setApplicantEmail("tenant@example.com");
        request.setFeeAmount(new BigDecimal("50.00"));

        when(applicationFeeService.createApplicationFee(any(ApplicationFeeRequest.class)))
                .thenThrow(new BadRequestException("Application fee already exists for application " + APP_ID));

        mockMvc.perform(post("/api/v1/application-fees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/application-fees/{id}/process-payment — should process payment")
    void shouldProcessPayment() throws Exception {
        sampleFee.setPaymentStatus(PaymentStatusEnum.COMPLETED);
        ApplicationFeeProcessPaymentRequest request = new ApplicationFeeProcessPaymentRequest();
        request.setStripePaymentMethodId("pm_test_001");

        when(applicationFeeService.processPayment(eq(FEE_ID), any(ApplicationFeeProcessPaymentRequest.class)))
                .thenReturn(sampleFee);

        mockMvc.perform(post("/api/v1/application-fees/{id}/process-payment", FEE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/application-fees/{id}/process-payment — should return 400 when already paid")
    void shouldReturn400WhenAlreadyPaid() throws Exception {
        ApplicationFeeProcessPaymentRequest request = new ApplicationFeeProcessPaymentRequest();
        request.setStripePaymentMethodId("pm_test_001");

        when(applicationFeeService.processPayment(eq(FEE_ID), any(ApplicationFeeProcessPaymentRequest.class)))
                .thenThrow(new BadRequestException("Fee is already paid"));

        mockMvc.perform(post("/api/v1/application-fees/{id}/process-payment", FEE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
