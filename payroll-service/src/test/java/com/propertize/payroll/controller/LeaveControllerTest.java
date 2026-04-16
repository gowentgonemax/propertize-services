package com.propertize.payroll.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertize.payroll.entity.LeaveBalanceEntity;
import com.propertize.payroll.entity.LeaveRequest;
import com.propertize.payroll.service.LeaveService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = LeaveController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class
})
class LeaveControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LeaveService leaveService;

    private UUID leaveRequestId;
    private LeaveRequest leaveRequest;

    @BeforeEach
    void setUp() {
        leaveRequestId = UUID.randomUUID();
        leaveRequest = new LeaveRequest();
        leaveRequest.setId(leaveRequestId);
    }

    @Test
    void getLeaveRequest_returns200() throws Exception {
        when(leaveService.getLeaveRequest(leaveRequestId)).thenReturn(leaveRequest);

        mockMvc.perform(get("/api/v1/leave/requests/{id}", leaveRequestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leaveRequestId.toString()));
    }

    @Test
    void getEmployeeLeaveRequests_returnsPage() throws Exception {
        when(leaveService.getEmployeeLeaveRequests(eq("emp-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(leaveRequest)));

        mockMvc.perform(get("/api/v1/leave/requests/employee/emp-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(leaveRequestId.toString()));
    }

    @Test
    void approveLeaveRequest_returns200() throws Exception {
        UUID approverId = UUID.randomUUID();
        when(leaveService.approveLeaveRequest(leaveRequestId, approverId)).thenReturn(leaveRequest);

        mockMvc.perform(post("/api/v1/leave/requests/{id}/approve", leaveRequestId)
                .param("approverId", approverId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(leaveRequestId.toString()));
    }

    @Test
    void rejectLeaveRequest_returns200() throws Exception {
        UUID rejectorId = UUID.randomUUID();
        when(leaveService.rejectLeaveRequest(leaveRequestId, rejectorId, "Not eligible"))
                .thenReturn(leaveRequest);

        mockMvc.perform(post("/api/v1/leave/requests/{id}/reject", leaveRequestId)
                .param("rejectorId", rejectorId.toString())
                .param("reason", "Not eligible"))
                .andExpect(status().isOk());
    }

    @Test
    void getLeaveBalances_returnsList() throws Exception {
        LeaveBalanceEntity balance = new LeaveBalanceEntity();
        when(leaveService.getLeaveBalances("emp-1", 2026)).thenReturn(List.of(balance));

        mockMvc.perform(get("/api/v1/leave/balances/emp-1")
                .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getPendingLeaveRequests_returnsList() throws Exception {
        when(leaveService.getPendingLeaveRequests()).thenReturn(List.of(leaveRequest));

        mockMvc.perform(get("/api/v1/leave/requests/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(leaveRequestId.toString()));
    }
}
