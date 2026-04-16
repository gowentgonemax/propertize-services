package com.propertize.payroll.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertize.payroll.entity.PayrollRun;
import com.propertize.commons.enums.employee.PayrollStatusEnum;
import com.propertize.payroll.service.PayrollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PayrollController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
class PayrollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PayrollService payrollService;

    private UUID clientId;
    private UUID payrollId;
    private PayrollRun payrollRun;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        payrollId = UUID.randomUUID();
        payrollRun = new PayrollRun();
        payrollRun.setId(payrollId);
        payrollRun.setStatus(PayrollStatusEnum.DRAFT);
        payrollRun.setPayDate(LocalDate.of(2026, 1, 15));
    }

    @Test
    void getPayrollRuns_withoutDates_returnsPage() throws Exception {
        when(payrollService.getPayrollRunsByClient(eq(clientId), any()))
                .thenReturn(new PageImpl<>(List.of(payrollRun)));

        mockMvc.perform(get("/api/v1/clients/{clientId}/payroll", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(payrollId.toString()));
    }

    @Test
    void getPayrollRuns_withDateRange_returnsPage() throws Exception {
        when(payrollService.getPayrollRunsByDateRange(eq(clientId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(payrollRun));

        mockMvc.perform(get("/api/v1/clients/{clientId}/payroll", clientId)
                .param("startDate", "2026-01-01")
                .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
    }

    @Test
    void getPayrollRun_returns200() throws Exception {
        when(payrollService.getPayrollRunById(payrollId)).thenReturn(payrollRun);

        mockMvc.perform(get("/api/v1/clients/{clientId}/payroll/{payrollId}", clientId, payrollId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(payrollId.toString()));
    }

    @Test
    void createPayrollRun_returns201() throws Exception {
        payrollRun.setPayDate(LocalDate.of(2026, 2, 15));
        when(payrollService.createPayrollRun(any(PayrollRun.class))).thenReturn(payrollRun);

        mockMvc.perform(post("/api/v1/clients/{clientId}/payroll", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payrollRun)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(payrollId.toString()));
    }

    @Test
    void processPayrollRun_returns200() throws Exception {
        PayrollRun processed = new PayrollRun();
        processed.setId(payrollId);
        processed.setStatus(PayrollStatusEnum.PROCESSING);
        when(payrollService.processPayrollRun(payrollId)).thenReturn(processed);

        mockMvc.perform(post("/api/v1/clients/{clientId}/payroll/{payrollId}/process", clientId, payrollId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void approvePayrollRun_returns200() throws Exception {
        PayrollRun approved = new PayrollRun();
        approved.setId(payrollId);
        approved.setStatus(PayrollStatusEnum.APPROVED);
        when(payrollService.approvePayrollRun(eq(payrollId), anyString())).thenReturn(approved);

        mockMvc.perform(post("/api/v1/clients/{clientId}/payroll/{payrollId}/approve", clientId, payrollId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void batchProcessPayrollRuns_returns200() throws Exception {
        List<UUID> runIds = List.of(payrollId);
        List<Map<String, Object>> results = List.of(Map.of("id", payrollId.toString(), "status", "COMPLETED"));
        when(payrollService.batchProcessPayrollRuns(anyList()))
                .thenReturn(CompletableFuture.completedFuture(results));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/clients/{clientId}/payroll/batch/process", clientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(runIds)))
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }
}
