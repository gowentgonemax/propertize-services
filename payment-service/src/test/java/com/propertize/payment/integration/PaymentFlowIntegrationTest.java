package com.propertize.payment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertize.payment.dto.payment.request.*;
import com.propertize.payment.dto.payment.response.StripePaymentIntentResponse;
import com.propertize.payment.dto.payment.response.StripeRefundResponse;
import com.propertize.payment.enums.*;
import com.propertize.payment.repository.PaymentRepository;
import com.propertize.payment.service.payment.StripePaymentService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the full payment lifecycle:
 * Create → Process (Stripe mocked) → Refund
 *
 * Uses H2 in-memory DB (application-test.yml profile).
 * StripePaymentService is mocked to avoid live Stripe calls.
 * No @Transactional: MockMvc requests commit their own transactions (separate
 * thread).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Payment Flow Integration Tests")
class PaymentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private StripePaymentService stripePaymentService;

    private static final String ORG_ID = "org-integration-001";
    private static final String TENANT_ID = "tenant-int-001";
    private static final String LEASE_ID = "lease-int-001";
    private static final String BASE_URL = "/api/v1/payments";

    // ──────────────────────── Create Payment ────────────────────────

    @Nested
    @DisplayName("POST /api/v1/payments — Create")
    class CreatePaymentIntegration {

        @Test
        @WithMockUser
        @DisplayName("Should persist a new PENDING payment to DB")
        void shouldCreatePaymentAndPersist() throws Exception {
            PaymentCreateRequest req = buildCreateRequest(new BigDecimal("1500.00"));

            MvcResult result = mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("PENDING"))
                    .andExpect(jsonPath("$.data.organizationId").value(ORG_ID))
                    .andExpect(jsonPath("$.data.amount").value(1500.00))
                    .andReturn();

            // Verify it's actually in the DB
            String paymentId = extractId(result);
            assertThat(paymentRepository.findById(paymentId)).isPresent();
            assertThat(paymentRepository.findById(paymentId).get().getStatus())
                    .isEqualTo(PaymentStatusEnum.PENDING);
        }

        @Test
        @WithMockUser
        @DisplayName("Should default net amount when no discount or late fee")
        void shouldSetNetAmountEqualToAmount() throws Exception {
            PaymentCreateRequest req = buildCreateRequest(new BigDecimal("800.00"));

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.netAmount").value(800.00));
        }

        @Test
        @WithMockUser
        @DisplayName("Should reject request with missing required fields")
        void shouldRejectMissingFields() throws Exception {
            PaymentCreateRequest req = new PaymentCreateRequest(); // empty

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("Should reject amount of zero")
        void shouldRejectZeroAmount() throws Exception {
            PaymentCreateRequest req = buildCreateRequest(BigDecimal.ZERO);

            mockMvc.perform(post(BASE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ──────────────────────── Retrieve Payment ────────────────────────

    @Nested
    @DisplayName("GET /api/v1/payments — Retrieve")
    class RetrievePaymentIntegration {

        @Test
        @WithMockUser
        @DisplayName("Should retrieve payment by ID after creation")
        void shouldRetrieveById() throws Exception {
            String paymentId = createPaymentViaApi(new BigDecimal("900.00"));

            mockMvc.perform(get(BASE_URL + "/" + paymentId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(paymentId))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return 404 for non-existent payment")
        void shouldReturn404ForUnknownId() throws Exception {
            mockMvc.perform(get(BASE_URL + "/nonexistent-id"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser
        @DisplayName("Should list payments by tenant")
        void shouldListByTenant() throws Exception {
            createPaymentViaApi(new BigDecimal("500.00"));

            mockMvc.perform(get(BASE_URL + "/tenant/" + TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].tenantId").value(TENANT_ID));
        }

        @Test
        @WithMockUser
        @DisplayName("Should list payments by lease")
        void shouldListByLease() throws Exception {
            createPaymentViaApi(new BigDecimal("1200.00"));

            mockMvc.perform(get(BASE_URL + "/lease/" + LEASE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].leaseId").value(LEASE_ID));
        }

        @Test
        @WithMockUser
        @DisplayName("Should return paginated payments for organization")
        void shouldReturnPaginatedByOrg() throws Exception {
            createPaymentViaApi(new BigDecimal("300.00"));
            createPaymentViaApi(new BigDecimal("400.00"));

            mockMvc.perform(get(BASE_URL)
                    .param("organizationId", ORG_ID)
                    .param("page", "1")
                    .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    // ──────────────────────── Process Payment (E2E) ────────────────────────

    @Nested
    @DisplayName("POST /api/v1/payments/{id}/process — Stripe Processing")
    class ProcessPaymentIntegration {

        @Test
        @WithMockUser
        @DisplayName("Should mark payment COMPLETED when Stripe confirms succeeded")
        void shouldCompleteWhenStripeSucceeds() throws Exception {
            String paymentId = createPaymentViaApi(new BigDecimal("1200.00"));

            // Mock Stripe: create intent + confirm → succeeded
            StripePaymentIntentResponse createResp = mockIntent("pi_int_001", "requires_confirmation");
            StripePaymentIntentResponse confirmResp = mockIntent("pi_int_001", "succeeded");

            when(stripePaymentService.createPaymentIntent(any())).thenReturn(createResp);
            when(stripePaymentService.confirmPaymentIntent(eq("pi_int_001"), any())).thenReturn(confirmResp);

            PaymentProcessRequest processReq = new PaymentProcessRequest();
            processReq.setStripePaymentMethodId("pm_card_visa");

            mockMvc.perform(post(BASE_URL + "/" + paymentId + "/process")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(processReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.data.stripePaymentIntentId").value("pi_int_001"));

            // Verify status persisted to DB
            assertThat(paymentRepository.findById(paymentId))
                    .isPresent()
                    .get()
                    .matches(p -> p.getStatus() == PaymentStatusEnum.COMPLETED);
        }

        @Test
        @WithMockUser
        @DisplayName("Should mark payment PENDING when Stripe returns requires_action")
        void shouldStayPendingWhenStripeRequiresAction() throws Exception {
            String paymentId = createPaymentViaApi(new BigDecimal("600.00"));

            StripePaymentIntentResponse createResp = mockIntent("pi_int_002", "requires_confirmation");
            StripePaymentIntentResponse actionResp = mockIntent("pi_int_002", "requires_action");

            when(stripePaymentService.createPaymentIntent(any())).thenReturn(createResp);
            when(stripePaymentService.confirmPaymentIntent(eq("pi_int_002"), any())).thenReturn(actionResp);

            PaymentProcessRequest processReq = new PaymentProcessRequest();
            processReq.setStripePaymentMethodId("pm_3d_secure");

            mockMvc.perform(post(BASE_URL + "/" + paymentId + "/process")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(processReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @WithMockUser
        @DisplayName("Should mark payment FAILED when Stripe returns failed status")
        void shouldFailWhenStripeFails() throws Exception {
            String paymentId = createPaymentViaApi(new BigDecimal("750.00"));

            StripePaymentIntentResponse createResp = mockIntent("pi_int_003", "requires_confirmation");
            StripePaymentIntentResponse failedResp = mockIntent("pi_int_003", "failed");

            when(stripePaymentService.createPaymentIntent(any())).thenReturn(createResp);
            when(stripePaymentService.confirmPaymentIntent(eq("pi_int_003"), any())).thenReturn(failedResp);

            PaymentProcessRequest processReq = new PaymentProcessRequest();

            mockMvc.perform(post(BASE_URL + "/" + paymentId + "/process")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(processReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("FAILED"))
                    .andExpect(jsonPath("$.data.failureReason").exists());
        }

        @Test
        @WithMockUser
        @DisplayName("Should reject processing of already completed payment")
        void shouldRejectDoubleProcessing() throws Exception {
            String paymentId = createAndProcessPayment(new BigDecimal("500.00"), "pi_dup_001");

            PaymentProcessRequest processReq = new PaymentProcessRequest();
            processReq.setStripePaymentMethodId("pm_second_attempt");

            mockMvc.perform(post(BASE_URL + "/" + paymentId + "/process")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(processReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already completed")));
        }
    }

    // ──────────────────────── Refund Payment (E2E) ────────────────────────

    @Nested
    @DisplayName("POST /api/v1/payments/{id}/refund — Refund")
    class RefundPaymentIntegration {

        @Test
        @WithMockUser
        @DisplayName("Should mark payment REFUNDED after successful Stripe refund")
        void shouldRefundCompletedPayment() throws Exception {
            String paymentId = createAndProcessPayment(new BigDecimal("2000.00"), "pi_ref_001");

            when(stripePaymentService.createRefund(any())).thenReturn(new StripeRefundResponse());

            PaymentRefundRequest refundReq = new PaymentRefundRequest();
            refundReq.setRefundAmount(new BigDecimal("2000.00"));
            refundReq.setReason("Customer requested cancellation");

            mockMvc.perform(post(BASE_URL + "/" + paymentId + "/refund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(refundReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.status").value("REFUNDED"));

            // Verify REFUNDED status persisted to DB
            assertThat(paymentRepository.findById(paymentId))
                    .isPresent()
                    .get()
                    .matches(p -> p.getStatus() == PaymentStatusEnum.REFUNDED);
        }

        @Test
        @WithMockUser
        @DisplayName("Should reject refund of non-completed payment")
        void shouldRejectRefundOfPendingPayment() throws Exception {
            String paymentId = createPaymentViaApi(new BigDecimal("400.00"));

            PaymentRefundRequest refundReq = new PaymentRefundRequest();
            refundReq.setRefundAmount(new BigDecimal("400.00"));
            refundReq.setReason("Changed mind");

            mockMvc.perform(post(BASE_URL + "/" + paymentId + "/refund")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(refundReq)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message")
                            .value(org.hamcrest.Matchers.containsString("Only completed payments")));
        }
    }

    // ──────────────────────── Update Payment ────────────────────────

    @Nested
    @DisplayName("PATCH /api/v1/payments/{id} — Update")
    class UpdatePaymentIntegration {

        @Test
        @WithMockUser
        @DisplayName("Should update payment notes and status")
        void shouldUpdatePayment() throws Exception {
            String paymentId = createPaymentViaApi(new BigDecimal("650.00"));

            PaymentUpdateRequest updateReq = new PaymentUpdateRequest();
            updateReq.setNotes("Updated by property manager");
            updateReq.setStatus(PaymentStatusEnum.OVERDUE);

            mockMvc.perform(patch(BASE_URL + "/" + paymentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.notes").value("Updated by property manager"))
                    .andExpect(jsonPath("$.data.status").value("OVERDUE"));
        }
    }

    // ──────────────────────── Helpers ────────────────────────

    private PaymentCreateRequest buildCreateRequest(BigDecimal amount) {
        PaymentCreateRequest req = new PaymentCreateRequest();
        req.setOrganizationId(ORG_ID);
        req.setTenantId(TENANT_ID);
        req.setLeaseId(LEASE_ID);
        req.setAmount(amount);
        req.setPaymentDate(LocalDate.now());
        req.setPaymentCategory(PaymentCategoryEnum.TENANT_PAYMENT);
        req.setPaymentContext(PaymentContextEnum.TENANT);
        req.setPaymentMethod(PaymentMethodEnum.CREDIT_CARD);
        return req;
    }

    private String createPaymentViaApi(BigDecimal amount) throws Exception {
        PaymentCreateRequest req = buildCreateRequest(amount);
        MvcResult result = mockMvc.perform(post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    /** Create + fully process a payment to COMPLETED status. */
    private String createAndProcessPayment(BigDecimal amount, String piId) throws Exception {
        String paymentId = createPaymentViaApi(amount);

        StripePaymentIntentResponse createResp = mockIntent(piId, "requires_confirmation");
        StripePaymentIntentResponse confirmResp = mockIntent(piId, "succeeded");

        when(stripePaymentService.createPaymentIntent(any())).thenReturn(createResp);
        when(stripePaymentService.confirmPaymentIntent(eq(piId), any())).thenReturn(confirmResp);

        PaymentProcessRequest processReq = new PaymentProcessRequest();
        processReq.setStripePaymentMethodId("pm_card_visa");

        mockMvc.perform(post(BASE_URL + "/" + paymentId + "/process")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(processReq)))
                .andExpect(status().isCreated());

        return paymentId;
    }

    private StripePaymentIntentResponse mockIntent(String id, String status) {
        StripePaymentIntentResponse resp = new StripePaymentIntentResponse();
        resp.setId(id);
        resp.setStatus(status);
        resp.setAmount(100L);
        resp.setCurrency("usd");
        return resp;
    }

    private String extractId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").asText();
    }
}
