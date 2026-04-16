package com.propertize.payroll.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertize.payroll.dto.compensation.request.CompensationCreateRequest;
import com.propertize.payroll.dto.compensation.request.CompensationUpdateRequest;
import com.propertize.payroll.dto.compensation.response.CompensationHistoryResponse;
import com.propertize.payroll.dto.compensation.response.CompensationResponse;
import com.propertize.payroll.enums.CompensationTypeEnum;
import com.propertize.payroll.enums.PayFrequencyEnum;
import com.propertize.payroll.service.CompensationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CompensationController.class, excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
})
class CompensationControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private CompensationService compensationService;

        private UUID compId;
        private UUID employeeId;
        private CompensationResponse compensationResponse;

        @BeforeEach
        void setUp() {
                compId = UUID.randomUUID();
                employeeId = UUID.randomUUID();
                compensationResponse = CompensationResponse.builder()
                                .id(compId)
                                .employeeId(employeeId)
                                .compensationType(CompensationTypeEnum.HOURLY_WAGE.name())
                                .payFrequency(PayFrequencyEnum.BI_WEEKLY)
                                .hourlyRate(new BigDecimal("25.00"))
                                .build();
        }

        @Test
        void createCompensation_returns201() throws Exception {
                CompensationCreateRequest request = CompensationCreateRequest.builder()
                                .employeeId(employeeId)
                                .compensationType(CompensationTypeEnum.HOURLY_WAGE.name())
                                .payFrequency(PayFrequencyEnum.BI_WEEKLY)
                                .hourlyRate(new BigDecimal("25.00"))
                                .effectiveDate(LocalDate.now().plusMonths(1))
                                .build();
                when(compensationService.createCompensation(any(CompensationCreateRequest.class)))
                                .thenReturn(compensationResponse);

                mockMvc.perform(post("/api/v1/compensation")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(compId.toString()));
        }

        @Test
        void updateCompensation_returns200() throws Exception {
                CompensationUpdateRequest request = new CompensationUpdateRequest();
                request.setHourlyRate(new BigDecimal("30.00"));
                when(compensationService.updateCompensation(eq(compId), any(CompensationUpdateRequest.class)))
                                .thenReturn(compensationResponse);

                mockMvc.perform(put("/api/v1/compensation/{id}", compId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(compId.toString()));
        }

        @Test
        void getCompensationById_returns200() throws Exception {
                when(compensationService.getCompensationById(compId)).thenReturn(compensationResponse);

                mockMvc.perform(get("/api/v1/compensation/{id}", compId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(compId.toString()));
        }

        @Test
        void getCurrentCompensation_returns200() throws Exception {
                when(compensationService.getCurrentCompensation(employeeId)).thenReturn(compensationResponse);

                mockMvc.perform(get("/api/v1/compensation/employee/{employeeId}/current", employeeId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.employeeId").value(employeeId.toString()));
        }

        @Test
        void getCompensationHistory_returnsList() throws Exception {
                CompensationHistoryResponse history = CompensationHistoryResponse.builder()
                                .id(compId)
                                .compensationType(CompensationTypeEnum.HOURLY_WAGE.name())
                                .build();
                when(compensationService.getCompensationHistory(employeeId)).thenReturn(List.of(history));

                mockMvc.perform(get("/api/v1/compensation/employee/{employeeId}/history", employeeId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(compId.toString()));
        }

        @Test
        void deactivateCompensation_returns204() throws Exception {
                doNothing().when(compensationService).deactivateCompensation(eq(compId), any(LocalDate.class),
                                anyString());

                mockMvc.perform(delete("/api/v1/compensation/{id}/deactivate", compId)
                                .param("reason", "Role changed"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void deactivateCompensation_withEndDate_returns204() throws Exception {
                doNothing().when(compensationService).deactivateCompensation(eq(compId), any(LocalDate.class),
                                anyString());

                mockMvc.perform(delete("/api/v1/compensation/{id}/deactivate", compId)
                                .param("endDate", "2026-06-30")
                                .param("reason", "Left company"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void deleteCompensation_returns204() throws Exception {
                doNothing().when(compensationService).deleteCompensation(compId);

                mockMvc.perform(delete("/api/v1/compensation/{id}", compId))
                                .andExpect(status().isNoContent());
        }
}
