package com.propertize.payroll.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertize.commons.enums.employee.EmploymentTypeEnum;
import com.propertize.commons.enums.employee.PayFrequencyEnum;
import com.propertize.commons.enums.employee.PayTypeEnum;
import com.propertize.payroll.dto.employee.CreateEmployeeRequest;
import com.propertize.payroll.dto.employee.EmployeeDTO;
import com.propertize.payroll.dto.employee.UpdateEmployeeRequest;
import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.payroll.service.EmployeeEntityService;
import com.propertize.payroll.service.EmployeeSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EmployeeController.class, excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
})
class EmployeeControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private EmployeeEntityService employeeService;

        @MockBean
        private EmployeeSyncService employeeSyncService;

        private UUID employeeId;
        private UUID clientId;
        private EmployeeDTO employeeDTO;

        @BeforeEach
        void setUp() {
                employeeId = UUID.randomUUID();
                clientId = UUID.randomUUID();
                employeeDTO = EmployeeDTO.builder()
                                .id(employeeId)
                                .clientId(clientId)
                                .employeeNumber("EMP-001")
                                .firstName("John")
                                .lastName("Doe")
                                .build();
        }

        @Test
        void getEmployeesByClient_returnsPage() throws Exception {
                when(employeeService.getEmployeesByClient(eq(clientId), any()))
                                .thenReturn(new PageImpl<>(List.of(employeeDTO)));

                mockMvc.perform(get("/api/v1/clients/{clientId}/employees", clientId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].id").value(employeeId.toString()));
        }

        @Test
        void getEmployeeById_returns200() throws Exception {
                when(employeeService.getEmployeeById(employeeId)).thenReturn(employeeDTO);

                mockMvc.perform(get("/api/v1/employees/{id}", employeeId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstName").value("John"))
                                .andExpect(jsonPath("$.lastName").value("Doe"));
        }

        @Test
        void createEmployee_forClient_returns201() throws Exception {
                CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                                .clientId(clientId)
                                .employeeNumber("EMP-001")
                                .firstName("John")
                                .lastName("Doe")
                                .hireDate(LocalDate.of(2026, 1, 1))
                                .payType(PayTypeEnum.HOURLY)
                                .employmentType(EmploymentTypeEnum.FULL_TIME)
                                .payFrequency(PayFrequencyEnum.BI_WEEKLY)
                                .build();
                when(employeeService.createEmployee(any(CreateEmployeeRequest.class))).thenReturn(employeeDTO);

                mockMvc.perform(post("/api/v1/clients/{clientId}/employees", clientId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(employeeId.toString()));
        }

        @Test
        void createEmployeeDirect_returns201() throws Exception {
                CreateEmployeeRequest request = CreateEmployeeRequest.builder()
                                .clientId(clientId)
                                .employeeNumber("EMP-002")
                                .firstName("Jane")
                                .lastName("Smith")
                                .hireDate(LocalDate.of(2026, 1, 1))
                                .payType(PayTypeEnum.HOURLY)
                                .employmentType(EmploymentTypeEnum.FULL_TIME)
                                .payFrequency(PayFrequencyEnum.BI_WEEKLY)
                                .build();
                when(employeeService.createEmployee(any(CreateEmployeeRequest.class))).thenReturn(employeeDTO);

                mockMvc.perform(post("/api/v1/employees")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());
        }

        @Test
        void updateEmployee_returns200() throws Exception {
                UpdateEmployeeRequest request = new UpdateEmployeeRequest();
                request.setFirstName("Johnny");
                EmployeeDTO updated = EmployeeDTO.builder().id(employeeId).firstName("Johnny").build();
                when(employeeService.updateEmployee(eq(employeeId), any(UpdateEmployeeRequest.class)))
                                .thenReturn(updated);

                mockMvc.perform(put("/api/v1/employees/{id}", employeeId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.firstName").value("Johnny"));
        }

        @Test
        void terminateEmployee_returns204() throws Exception {
                doNothing().when(employeeService).terminateEmployee(eq(employeeId), any());

                mockMvc.perform(post("/api/v1/employees/{id}/terminate", employeeId)
                                .param("terminationDate", "2026-06-30"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void terminateEmployee_withoutDate_returns204() throws Exception {
                doNothing().when(employeeService).terminateEmployee(eq(employeeId), any());

                mockMvc.perform(post("/api/v1/employees/{id}/terminate", employeeId))
                                .andExpect(status().isNoContent());
        }

        @Test
        void syncEmployee_returns200() throws Exception {
                UUID orgClientId = UUID.randomUUID();
                when(employeeSyncService.syncEmployee(any(UUID.class), any(UUID.class), anyString()))
                                .thenReturn(new EmployeeEntity());

                mockMvc.perform(post("/api/v1/employees/{id}/sync", employeeId)
                                .param("clientId", orgClientId.toString())
                                .header("Authorization", "Bearer test-token"))
                                .andExpect(status().isOk());
        }

        @Test
        void syncAllEmployees_returns202() throws Exception {
                UUID organizationId = UUID.randomUUID();
                when(employeeSyncService.syncAllEmployees(any(UUID.class), any(UUID.class), anyString()))
                                .thenReturn(CompletableFuture.completedFuture(3));

                mockMvc.perform(post("/api/v1/clients/{clientId}/employees/sync", clientId)
                                .param("organizationId", organizationId.toString())
                                .header("Authorization", "Bearer test-token"))
                                .andExpect(status().isAccepted());
        }

        @Test
        void getEmployeeByNumber_returns200() throws Exception {
                when(employeeService.getEmployeeByNumber("EMP-001")).thenReturn(employeeDTO);

                mockMvc.perform(get("/api/v1/employees/by-number/EMP-001"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.employeeNumber").value("EMP-001"));
        }

        @Test
        void getActiveEmployeesByClient_returnsList() throws Exception {
                when(employeeService.getActiveEmployeesByClient(clientId)).thenReturn(List.of(employeeDTO));

                mockMvc.perform(get("/api/v1/employees/client/{clientId}/active", clientId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(employeeId.toString()));
        }

        @Test
        void searchEmployees_returnsPage() throws Exception {
                when(employeeService.searchEmployees(eq(clientId), eq("John"), any(Pageable.class)))
                                .thenReturn(new PageImpl<>(List.of(employeeDTO)));

                mockMvc.perform(get("/api/v1/employees/client/{clientId}/search", clientId)
                                .param("query", "John"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].firstName").value("John"));
        }
}
