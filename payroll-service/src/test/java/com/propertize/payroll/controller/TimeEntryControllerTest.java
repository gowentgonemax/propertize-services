package com.propertize.payroll.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertize.payroll.dto.timeentry.CreateTimeEntryRequest;
import com.propertize.payroll.dto.timeentry.TimeEntryDTO;
import com.propertize.payroll.enums.TimeEntryStatusEnum;
import com.propertize.payroll.service.TimeEntryService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TimeEntryController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
class TimeEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TimeEntryService timeEntryService;

    private UUID entryId;
    private UUID employeeId;
    private UUID clientId;
    private TimeEntryDTO timeEntryDTO;

    @BeforeEach
    void setUp() {
        entryId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        timeEntryDTO = TimeEntryDTO.builder()
                .id(entryId)
                .employeeId(employeeId)
                .workDate(LocalDate.of(2026, 1, 6))
                .regularHours(new BigDecimal("8.0"))
                .status(TimeEntryStatusEnum.PENDING)
                .build();
    }

    @Test
    void createTimeEntry_returns201() throws Exception {
        CreateTimeEntryRequest request = CreateTimeEntryRequest.builder()
                .employeeId(employeeId)
                .workDate(LocalDate.of(2026, 1, 6))
                .regularHours(new BigDecimal("8.0"))
                .build();
        when(timeEntryService.createTimeEntry(any(CreateTimeEntryRequest.class))).thenReturn(timeEntryDTO);

        mockMvc.perform(post("/api/v1/time-entries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(entryId.toString()));
    }

    @Test
    void getTimeEntry_returns200() throws Exception {
        when(timeEntryService.getTimeEntryById(entryId)).thenReturn(timeEntryDTO);

        mockMvc.perform(get("/api/v1/time-entries/{id}", entryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(entryId.toString()));
    }

    @Test
    void getTimeEntriesByEmployee_returnsPage() throws Exception {
        when(timeEntryService.getTimeEntriesByEmployee(eq(employeeId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(timeEntryDTO)));

        mockMvc.perform(get("/api/v1/time-entries/employee/{employeeId}", employeeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].employeeId").value(employeeId.toString()));
    }

    @Test
    void getTimeEntriesByEmployeeAndDateRange_returnsList() throws Exception {
        when(timeEntryService.getTimeEntriesByEmployeeAndDateRange(
                eq(employeeId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(timeEntryDTO));

        mockMvc.perform(get("/api/v1/time-entries/employee/{employeeId}/range", employeeId)
                .param("startDate", "2026-01-01")
                .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(entryId.toString()));
    }

    @Test
    void getTimeEntriesByClientAndDateRange_returnsList() throws Exception {
        when(timeEntryService.getTimeEntriesByClientAndDateRange(
                eq(clientId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(timeEntryDTO));

        mockMvc.perform(get("/api/v1/time-entries/client/{clientId}/range", clientId)
                .param("startDate", "2026-01-01")
                .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(entryId.toString()));
    }

    @Test
    void getPendingByClient_returnsPage() throws Exception {
        when(timeEntryService.getPendingTimeEntriesByClient(eq(clientId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(timeEntryDTO)));

        mockMvc.perform(get("/api/v1/time-entries/client/{clientId}/pending", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(entryId.toString()));
    }

    @Test
    void approveTimeEntry_returns200() throws Exception {
        UUID approverId = UUID.randomUUID();
        TimeEntryDTO approved = TimeEntryDTO.builder().id(entryId).status(TimeEntryStatusEnum.APPROVED).build();
        when(timeEntryService.approveTimeEntry(entryId, approverId)).thenReturn(approved);

        mockMvc.perform(post("/api/v1/time-entries/{id}/approve", entryId)
                .param("approverId", approverId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void rejectTimeEntry_returns200() throws Exception {
        TimeEntryDTO rejected = TimeEntryDTO.builder().id(entryId).status(TimeEntryStatusEnum.REJECTED).build();
        when(timeEntryService.rejectTimeEntry(entryId, "Invalid hours")).thenReturn(rejected);

        mockMvc.perform(post("/api/v1/time-entries/{id}/reject", entryId)
                .param("rejectionNotes", "Invalid hours"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));
    }

    @Test
    void deleteTimeEntry_returns200() throws Exception {
        doNothing().when(timeEntryService).deleteTimeEntry(entryId);

        mockMvc.perform(delete("/api/v1/time-entries/{id}", entryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getTotalApprovedHours_returnsHoursMap() throws Exception {
        when(timeEntryService.getTotalApprovedRegularHours(eq(employeeId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("40.0"));
        when(timeEntryService.getTotalApprovedOvertimeHours(eq(employeeId), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("5.0"));

        mockMvc.perform(get("/api/v1/time-entries/employee/{employeeId}/hours", employeeId)
                .param("startDate", "2026-01-01")
                .param("endDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.regularHours").value(40.0))
                .andExpect(jsonPath("$.data.overtimeHours").value(5.0))
                .andExpect(jsonPath("$.data.totalHours").value(45.0));
    }
}
