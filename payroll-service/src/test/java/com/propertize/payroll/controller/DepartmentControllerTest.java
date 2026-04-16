package com.propertize.payroll.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertize.payroll.dto.department.request.DepartmentCreateRequest;
import com.propertize.payroll.dto.department.response.DepartmentResponse;
import com.propertize.payroll.service.DepartmentService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DepartmentController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DepartmentService departmentService;

    private UUID deptId;
    private UUID clientId;
    private DepartmentResponse deptResponse;

    @BeforeEach
    void setUp() {
        deptId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        deptResponse = DepartmentResponse.builder()
                .id(deptId)
                .name("Engineering")
                .departmentCode("ENG")
                .isActive(true)
                .build();
    }

    @Test
    void createDepartment_returns201() throws Exception {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .clientId(clientId)
                .name("Engineering")
                .departmentCode("ENG")
                .build();
        when(departmentService.createDepartment(any(DepartmentCreateRequest.class))).thenReturn(deptResponse);

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(deptId.toString()))
                .andExpect(jsonPath("$.name").value("Engineering"));
    }

    @Test
    void createDepartment_missingName_returns400() throws Exception {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .clientId(clientId)
                // name missing intentionally
                .departmentCode("ENG")
                .build();

        mockMvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDepartment_returns200() throws Exception {
        when(departmentService.getDepartment(deptId)).thenReturn(deptResponse);

        mockMvc.perform(get("/api/v1/departments/{id}", deptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentCode").value("ENG"));
    }

    @Test
    void getAllDepartments_returnsPage() throws Exception {
        when(departmentService.getAllDepartments(eq(clientId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deptResponse)));

        mockMvc.perform(get("/api/v1/departments/client/{clientId}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(deptId.toString()));
    }

    @Test
    void getActiveDepartments_returnsList() throws Exception {
        when(departmentService.getActiveDepartments(clientId)).thenReturn(List.of(deptResponse));

        mockMvc.perform(get("/api/v1/departments/client/{clientId}/active", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    void updateDepartment_returns200() throws Exception {
        DepartmentCreateRequest request = DepartmentCreateRequest.builder()
                .clientId(clientId)
                .name("Engineering Updated")
                .departmentCode("ENG")
                .build();
        DepartmentResponse updated = DepartmentResponse.builder().id(deptId).name("Engineering Updated").build();
        when(departmentService.updateDepartment(eq(deptId), any())).thenReturn(updated);

        mockMvc.perform(put("/api/v1/departments/{id}", deptId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Engineering Updated"));
    }

    @Test
    void deleteDepartment_returns204() throws Exception {
        doNothing().when(departmentService).deleteDepartment(deptId);

        mockMvc.perform(delete("/api/v1/departments/{id}", deptId))
                .andExpect(status().isNoContent());
    }
}
