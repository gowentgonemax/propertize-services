package com.propertize.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.propertize.payment.dto.payment.request.*;
import com.propertize.payment.entity.Payment;
import com.propertize.commons.enums.payment.PaymentCategoryEnum;
import com.propertize.commons.enums.payment.PaymentContextEnum;
import com.propertize.commons.enums.payment.PaymentStatusEnum;
import com.propertize.commons.exception.ResourceNotFoundException;
import com.propertize.payment.service.PaymentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@DisplayName("PaymentController Tests")
class PaymentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private PaymentService paymentService;

        private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        private Payment samplePayment;

        @BeforeEach
        void setUp() {
                samplePayment = new Payment();
                samplePayment.setId("pay-001");
                samplePayment.setOrganizationId("org-001");
                samplePayment.setTenantId("tenant-001");
                samplePayment.setAmount(new BigDecimal("1200.00"));
                samplePayment.setNetAmount(new BigDecimal("1200.00"));
                samplePayment.setPaymentDate(LocalDate.of(2026, 3, 27));
                samplePayment.setPaymentCategory(PaymentCategoryEnum.TENANT_PAYMENT);
                samplePayment.setPaymentContext(PaymentContextEnum.TENANT);
                samplePayment.setStatus(PaymentStatusEnum.PENDING);
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/v1/payments — should return paginated payments")
        void shouldGetAllPayments() throws Exception {
                Page<Payment> page = new PageImpl<>(List.of(samplePayment));
                when(paymentService.getAllPayments("org-001", 1, 20)).thenReturn(page);

                mockMvc.perform(get("/api/v1/payments")
                                .param("organizationId", "org-001")
                                .param("page", "1")
                                .param("size", "20"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/v1/payments/{id} — should return payment by ID")
        void shouldGetPaymentById() throws Exception {
                when(paymentService.getPaymentById("pay-001")).thenReturn(samplePayment);

                mockMvc.perform(get("/api/v1/payments/pay-001"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.id").value("pay-001"));
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/v1/payments/{id} — should return 404 for non-existent payment")
        void shouldReturn404WhenNotFound() throws Exception {
                when(paymentService.getPaymentById("bad-id"))
                                .thenThrow(new ResourceNotFoundException("Payment", "id", "bad-id"));

                mockMvc.perform(get("/api/v1/payments/bad-id"))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/v1/payments/tenant/{tenantId} — should return tenant payments")
        void shouldGetPaymentsByTenant() throws Exception {
                when(paymentService.getPaymentsByTenant("tenant-001")).thenReturn(List.of(samplePayment));

                mockMvc.perform(get("/api/v1/payments/tenant/tenant-001"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @WithMockUser
        @DisplayName("GET /api/v1/payments/lease/{leaseId} — should return lease payments")
        void shouldGetPaymentsByLease() throws Exception {
                when(paymentService.getPaymentsByLease("lease-001")).thenReturn(List.of(samplePayment));

                mockMvc.perform(get("/api/v1/payments/lease/lease-001"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser
        @DisplayName("POST /api/v1/payments — should create payment")
        void shouldCreatePayment() throws Exception {
                PaymentCreateRequest request = new PaymentCreateRequest();
                request.setOrganizationId("org-001");
                request.setAmount(new BigDecimal("1200.00"));
                request.setPaymentDate(LocalDate.of(2026, 3, 27));
                request.setPaymentCategory(PaymentCategoryEnum.TENANT_PAYMENT);
                request.setPaymentContext(PaymentContextEnum.TENANT);

                when(paymentService.createPayment(any(PaymentCreateRequest.class))).thenReturn(samplePayment);

                mockMvc.perform(post("/api/v1/payments")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser
        @DisplayName("POST /api/v1/payments — should reject missing required fields")
        void shouldRejectInvalidCreateRequest() throws Exception {
                PaymentCreateRequest request = new PaymentCreateRequest();
                // Missing required fields

                mockMvc.perform(post("/api/v1/payments")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("POST /api/v1/payments/{id}/process — should process payment")
        void shouldProcessPayment() throws Exception {
                samplePayment.setStatus(PaymentStatusEnum.COMPLETED);
                when(paymentService.processPayment(eq("pay-001"), any(PaymentProcessRequest.class)))
                                .thenReturn(samplePayment);

                PaymentProcessRequest request = new PaymentProcessRequest();
                request.setStripePaymentMethodId("pm_test");

                mockMvc.perform(post("/api/v1/payments/pay-001/process")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser
        @DisplayName("POST /api/v1/payments/{id}/refund — should refund payment")
        void shouldRefundPayment() throws Exception {
                samplePayment.setStatus(PaymentStatusEnum.REFUNDED);
                when(paymentService.refundPayment(eq("pay-001"), any(PaymentRefundRequest.class)))
                                .thenReturn(samplePayment);

                PaymentRefundRequest request = new PaymentRefundRequest();
                request.setRefundAmount(new BigDecimal("1200.00"));

                mockMvc.perform(post("/api/v1/payments/pay-001/refund")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser
        @DisplayName("PATCH /api/v1/payments/{id} — should update payment")
        void shouldUpdatePayment() throws Exception {
                samplePayment.setNotes("Updated");
                when(paymentService.updatePayment(eq("pay-001"), any(PaymentUpdateRequest.class)))
                                .thenReturn(samplePayment);

                PaymentUpdateRequest request = new PaymentUpdateRequest();
                request.setNotes("Updated");

                mockMvc.perform(patch("/api/v1/payments/pay-001")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("Endpoints should require authentication")
        void shouldRequireAuthentication() throws Exception {
                mockMvc.perform(get("/api/v1/payments").param("organizationId", "org-001"))
                                .andExpect(status().isUnauthorized());
        }
}
