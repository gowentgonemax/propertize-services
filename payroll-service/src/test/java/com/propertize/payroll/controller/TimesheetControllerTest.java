package com.propertize.payroll.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertize.payroll.dto.timesheet.response.TimesheetResponse;
import com.propertize.payroll.enums.TimesheetStatusEnum;
import com.propertize.payroll.service.TimesheetService;
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
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TimesheetController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
class TimesheetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TimesheetService timesheetService;

    private UUID timesheetId;
    private TimesheetResponse timesheetResponse;

    @BeforeEach
    void setUp() {
        timesheetId = UUID.randomUUID();
        timesheetResponse = TimesheetResponse.builder()
                .id(timesheetId)
                .employeeId("emp-1")
                .employeeName("John Doe")
                .weekStartDate(LocalDate.of(2026, 1, 5))
                .weekEndDate(LocalDate.of(2026, 1, 11))
                .status(TimesheetStatusEnum.DRAFT)
                .build();
    }

    @Test
    void getAllTimesheets_returnsPage() throws Exception {
        when(timesheetService.getAllTimesheets(any())).thenReturn(new PageImpl<>(List.of(timesheetResponse)));

        mockMvc.perform(get("/api/v1/timesheets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(timesheetId.toString()));
    }

    @Test
    void getTimesheet_returnsTimesheet() throws Exception {
        when(timesheetService.getTimesheet(timesheetId)).thenReturn(timesheetResponse);

        mockMvc.perform(get("/api/v1/timesheets/{id}", timesheetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(timesheetId.toString()));
    }

    @Test
    void getEmployeeTimesheets_returnsPage() throws Exception {
        when(timesheetService.getEmployeeTimesheets(eq("emp-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(timesheetResponse)));

        mockMvc.perform(get("/api/v1/timesheets/employee/emp-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeeId").value("emp-1"));
    }

    @Test
    void submitTimesheet_returnsUpdatedTimesheet() throws Exception {
        TimesheetResponse submitted = TimesheetResponse.builder()
                .id(timesheetId)
                .status(TimesheetStatusEnum.SUBMITTED)
                .build();
        when(timesheetService.submitTimesheet(timesheetId)).thenReturn(submitted);

        mockMvc.perform(post("/api/v1/timesheets/{id}/submit", timesheetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    void approveTimesheet_returnsApproved() throws Exception {
        UUID approverId = UUID.randomUUID();
        TimesheetResponse approved = TimesheetResponse.builder()
                .id(timesheetId)
                .status(TimesheetStatusEnum.APPROVED)
                .build();
        when(timesheetService.approveTimesheet(timesheetId, approverId)).thenReturn(approved);

        mockMvc.perform(post("/api/v1/timesheets/{id}/approve", timesheetId)
                .param("approverId", approverId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void rejectTimesheet_returnsRejected() throws Exception {
        UUID rejectorId = UUID.randomUUID();
        TimesheetResponse rejected = TimesheetResponse.builder()
                .id(timesheetId)
                .status(TimesheetStatusEnum.REJECTED)
                .rejectionReason("Hours not accurate")
                .build();
        when(timesheetService.rejectTimesheet(timesheetId, rejectorId, "Hours not accurate")).thenReturn(rejected);

        mockMvc.perform(post("/api/v1/timesheets/{id}/reject", timesheetId)
                .param("rejectorId", rejectorId.toString())
                .param("reason", "Hours not accurate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void getPendingApprovals_returnsList() throws Exception {
        UUID clientId = UUID.randomUUID();
        when(timesheetService.getPendingApprovals(clientId)).thenReturn(List.of(timesheetResponse));

        mockMvc.perform(get("/api/v1/timesheets/pending/{clientId}", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(timesheetId.toString()));
    }

    @Test
    void getTimesheetsByDateRange_returnsList() throws Exception {
        when(timesheetService.getTimesheetsByDateRange(eq("emp-1"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(timesheetResponse));

        mockMvc.perform(get("/api/v1/timesheets/employee/emp-1/range")
                .param("startDate", "2026-01-01")
                .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value("emp-1"));
    }

    @Test
    void getTimesheetsBulkByDateRange_returnsList() throws Exception {
        when(timesheetService.getTimesheetsByEmployeesAndDateRange(anyList(), any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(List.of(timesheetResponse));

        String requestBody = objectMapper.writeValueAsString(
                Map.of("employeeIds", List.of("emp-1"), "startDate", "2026-01-01", "endDate", "2026-01-31"));

        mockMvc.perform(post("/api/v1/timesheets/bulk/range")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(timesheetId.toString()));
    }
}
