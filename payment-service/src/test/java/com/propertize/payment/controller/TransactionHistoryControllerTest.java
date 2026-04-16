package com.propertize.payment.controller;

import com.propertize.payment.entity.TransactionHistory;
import com.propertize.commons.exception.ResourceNotFoundException;
import com.propertize.payment.service.TransactionHistoryService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TransactionHistoryController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
@DisplayName("TransactionHistoryController Tests")
class TransactionHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionHistoryService transactionHistoryService;

    private TransactionHistory sampleTxn;
    private static final String ORG_ID = "org-001";
    private static final String TXN_ID = "txn-001";
    private static final String REF_NUMBER = "TXN-20260401-001";

    @BeforeEach
    void setUp() {
        sampleTxn = new TransactionHistory();
        sampleTxn.setId(TXN_ID);
        sampleTxn.setOrganizationId(ORG_ID);
        sampleTxn.setReferenceNumber(REF_NUMBER);
        sampleTxn.setAmount(new BigDecimal("1200.00"));
    }

    @Test
    @DisplayName("GET /api/v1/transactions — should return paginated transactions")
    void shouldGetTransactionsByOrganization() throws Exception {
        Page<TransactionHistory> page = new PageImpl<>(List.of(sampleTxn));
        when(transactionHistoryService.getTransactionsByOrganization(ORG_ID, 1, 20)).thenReturn(page);

        mockMvc.perform(get("/api/v1/transactions")
                .param("organizationId", ORG_ID)
                .param("page", "1")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{id} — should return transaction by id")
    void shouldGetTransactionById() throws Exception {
        when(transactionHistoryService.getTransactionById(TXN_ID)).thenReturn(sampleTxn);

        mockMvc.perform(get("/api/v1/transactions/{id}", TXN_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(TXN_ID));
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{id} — should return 404 when not found")
    void shouldReturn404WhenNotFound() throws Exception {
        when(transactionHistoryService.getTransactionById("bad-id"))
                .thenThrow(new ResourceNotFoundException("Transaction", "id", "bad-id"));

        mockMvc.perform(get("/api/v1/transactions/bad-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/transactions/reference/{referenceNumber} — should return by reference number")
    void shouldGetTransactionByReferenceNumber() throws Exception {
        when(transactionHistoryService.getTransactionByReferenceNumber(REF_NUMBER)).thenReturn(sampleTxn);

        mockMvc.perform(get("/api/v1/transactions/reference/{ref}", REF_NUMBER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.referenceNumber").value(REF_NUMBER));
    }

    @Test
    @DisplayName("GET /api/v1/transactions/reference/{referenceNumber} — should return 404 when not found")
    void shouldReturn404WhenRefNotFound() throws Exception {
        when(transactionHistoryService.getTransactionByReferenceNumber("BAD-REF"))
                .thenThrow(new ResourceNotFoundException("Transaction", "referenceNumber", "BAD-REF"));

        mockMvc.perform(get("/api/v1/transactions/reference/BAD-REF"))
                .andExpect(status().isNotFound());
    }
}
