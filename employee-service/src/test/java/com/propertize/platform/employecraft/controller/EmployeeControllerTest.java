package com.propertize.platform.employecraft.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertize.platform.employecraft.dto.employee.request.EmployeeCreateRequest;
import com.propertize.platform.employecraft.dto.employee.response.EmployeePayrollSummary;
import com.propertize.platform.employecraft.dto.employee.response.EmployeeResponse;
import com.propertize.platform.employecraft.enums.EmployeeStatusEnum;
import com.propertize.platform.employecraft.enums.EmploymentTypeEnum;
import com.propertize.commons.enums.employee.PayTypeEnum;
import com.propertize.platform.employecraft.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EmployeeController.class, excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
})
@TestPropertySource(properties = { "propertize.api.url=localhost:8082" })
class EmployeeControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private EmployeeService employeeService;

        private UUID employeeId;
        private EmployeeResponse employeeResponse;

        @BeforeEach
        void setUp() {
                employeeId = UUID.randomUUID();
                employeeResponse = EmployeeResponse.builder()
                                .id(employeeId)
                                .firstName("John")
                                .lastName("Doe")
                                .email("john.doe@example.com")
                                .employmentType(EmploymentTypeEnum.FULL_TIME)
                                .status(EmployeeStatusEnum.ACTIVE)
                                .hireDate(LocalDate.of(2023, 1, 15))
                                .employeeNumber("EMP-20230115-0001")
                                .build();
        }

        @Test
        void getAllEmployees_returns200() throws Exception {
                when(employeeService.getAllEmployees(any(), any()))
                                .thenReturn(new PageImpl<>(List.of(employeeResponse)));

                mockMvc.perform(get("/api/v1/employees"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].id").value(employeeId.toString()));
        }

        @Test
        void getAllEmployees_withOrganizationId_returns200() throws Exception {
                UUID orgId = UUID.randomUUID();
                when(employeeService.getAllEmployees(eq(orgId), any()))
                                .thenReturn(new PageImpl<>(List.of(employeeResponse)));

                mockMvc.perform(get("/api/v1/employees").param("organizationId", orgId.toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].firstName").value("John"));
        }

        @Test
        void getEmployee_returns200() throws Exception {
                when(employeeService.getEmployee(employeeId)).thenReturn(employeeResponse);

                mockMvc.perform(get("/api/v1/employees/{id}", employeeId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(employeeId.toString()))
                                .andExpect(jsonPath("$.firstName").value("John"));
        }

        @Test
        void getEmployeeByUserId_returns200() throws Exception {
                when(employeeService.getEmployeeByUserId(42L)).thenReturn(employeeResponse);

                mockMvc.perform(get("/api/v1/employees/by-user/{userId}", 42L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
        }

        @Test
        void getMyEmployeeProfile_whenPresent_returns200() throws Exception {
                when(employeeService.getMyEmployeeProfile()).thenReturn(Optional.of(employeeResponse));

                mockMvc.perform(get("/api/v1/employees/me"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(employeeId.toString()));
        }

        @Test
        void getMyEmployeeProfile_whenAbsent_returns204() throws Exception {
                when(employeeService.getMyEmployeeProfile()).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/v1/employees/me"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void createEmployee_returns201() throws Exception {
                EmployeeCreateRequest request = EmployeeCreateRequest.builder()
                                .firstName("John")
                                .lastName("Doe")
                                .email("john.doe@example.com")
                                .employmentType(EmploymentTypeEnum.FULL_TIME)
                                .hireDate(LocalDate.of(2025, 6, 1))
                                .build();

                when(employeeService.createEmployee(any(EmployeeCreateRequest.class), any()))
                                .thenReturn(employeeResponse);

                mockMvc.perform(post("/api/v1/employees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(employeeId.toString()));
        }

        @Test
        void activateEmployee_returns200() throws Exception {
                employeeResponse.setStatus(EmployeeStatusEnum.ACTIVE);
                when(employeeService.activateEmployee(employeeId)).thenReturn(employeeResponse);

                mockMvc.perform(post("/api/v1/employees/{id}/activate", employeeId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        void terminateEmployee_returns200() throws Exception {
                employeeResponse.setStatus(EmployeeStatusEnum.TERMINATED);
                when(employeeService.terminateEmployee(eq(employeeId), anyString())).thenReturn(employeeResponse);

                mockMvc.perform(post("/api/v1/employees/{id}/terminate", employeeId)
                                .param("reason", "Resignation"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("TERMINATED"));
        }

        @Test
        void getPayrollSummaries_returns200() throws Exception {
                EmployeePayrollSummary summary = EmployeePayrollSummary.builder()
                                .id(employeeId)
                                .employeeNumber("EMP-20230115-0001")
                                .firstName("John")
                                .lastName("Doe")
                                .payType(PayTypeEnum.SALARY.name())
                                .payRate(new BigDecimal("75000.00"))
                                .build();

                when(employeeService.getPayrollSummaries()).thenReturn(List.of(summary));

                mockMvc.perform(get("/api/v1/employees/payroll-summary"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(employeeId.toString()))
                                .andExpect(jsonPath("$[0].employeeNumber").value("EMP-20230115-0001"));
        }

        @Test
        void getChangedSince_returns200() throws Exception {
                when(employeeService.getChangedSince(any(), any()))
                                .thenReturn(new PageImpl<>(List.of(employeeResponse)));

                mockMvc.perform(get("/api/v1/employees/changed-since")
                                .param("since", "2025-01-01T00:00:00"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].id").value(employeeId.toString()));
        }
}
