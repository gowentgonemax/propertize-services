package com.propertize.payroll.service;

import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.payroll.entity.LeaveBalanceEntity;
import com.propertize.payroll.entity.LeaveRequest;
import com.propertize.payroll.enums.LeaveStatusEnum;
import com.propertize.payroll.enums.LeaveTypeEnum;
import com.propertize.payroll.repository.LeaveBalanceRepository;
import com.propertize.payroll.repository.LeaveRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    LeaveRequestRepository leaveRequestRepository;
    @Mock
    LeaveBalanceRepository leaveBalanceRepository;

    @InjectMocks
    LeaveService leaveService;

    UUID leaveRequestId;
    UUID employeeId;
    UUID approverId;
    EmployeeEntity employee;

    @BeforeEach
    void setUp() {
        leaveRequestId = UUID.randomUUID();
        employeeId = UUID.randomUUID();
        approverId = UUID.randomUUID();

        employee = new EmployeeEntity();
        employee.setId(employeeId);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private LeaveRequest buildPendingRequest() {
        LeaveRequest request = new LeaveRequest();
        request.setId(leaveRequestId);
        request.setEmployee(employee);
        request.setLeaveType(LeaveTypeEnum.VACATION);
        request.setStartDate(LocalDate.of(2025, 6, 1));
        request.setEndDate(LocalDate.of(2025, 6, 5));
        request.setDaysRequested(BigDecimal.valueOf(5));
        request.setHoursRequested(BigDecimal.valueOf(40));
        request.setStatus(LeaveStatusEnum.PENDING);
        request.setReason("Family vacation");
        return request;
    }

    private LeaveRequest buildApprovedRequest() {
        LeaveRequest request = buildPendingRequest();
        request.setStatus(LeaveStatusEnum.APPROVED);
        request.setApprovedBy(approverId.toString());
        return request;
    }

    private LeaveBalanceEntity buildLeaveBalance(LeaveTypeEnum type, int year) {
        LeaveBalanceEntity balance = new LeaveBalanceEntity();
        balance.setId(UUID.randomUUID());
        balance.setEmployee(employee);
        balance.setLeaveType(type);
        balance.setYear(year);
        balance.setBeginningBalance(BigDecimal.valueOf(80));
        balance.setAccruedHours(BigDecimal.valueOf(40));
        balance.setUsedHours(BigDecimal.valueOf(16));
        balance.setAdjustedHours(BigDecimal.ZERO);
        balance.setCarriedOverHours(BigDecimal.ZERO);
        return balance;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getLeaveRequest
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getLeaveRequest_returnsRequest_whenFound() {
        LeaveRequest request = buildPendingRequest();
        when(leaveRequestRepository.findById(leaveRequestId)).thenReturn(Optional.of(request));

        LeaveRequest result = leaveService.getLeaveRequest(leaveRequestId);

        assertThat(result.getId()).isEqualTo(leaveRequestId);
        assertThat(result.getStatus()).isEqualTo(LeaveStatusEnum.PENDING);
        assertThat(result.getLeaveType()).isEqualTo(LeaveTypeEnum.VACATION);
        verify(leaveRequestRepository).findById(leaveRequestId);
    }

    @Test
    void getLeaveRequest_throwsEntityNotFound_whenMissing() {
        when(leaveRequestRepository.findById(leaveRequestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.getLeaveRequest(leaveRequestId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Leave request not found")
                .hasMessageContaining(leaveRequestId.toString());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getEmployeeLeaveRequests
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getEmployeeLeaveRequests_returnsPage() {
        LeaveRequest request = buildPendingRequest();
        Page<LeaveRequest> page = new PageImpl<>(List.of(request));
        when(leaveRequestRepository.findByEmployeeId(eq(employeeId), any(Pageable.class)))
                .thenReturn(page);

        Page<LeaveRequest> result = leaveService.getEmployeeLeaveRequests(
                employeeId.toString(), Pageable.unpaged());

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(leaveRequestId);
        verify(leaveRequestRepository).findByEmployeeId(eq(employeeId), any(Pageable.class));
    }

    @Test
    void getEmployeeLeaveRequests_returnsEmptyPage_whenNoRequests() {
        Page<LeaveRequest> emptyPage = new PageImpl<>(Collections.emptyList());
        when(leaveRequestRepository.findByEmployeeId(eq(employeeId), any(Pageable.class)))
                .thenReturn(emptyPage);

        Page<LeaveRequest> result = leaveService.getEmployeeLeaveRequests(
                employeeId.toString(), Pageable.unpaged());

        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void getEmployeeLeaveRequests_throwsException_whenInvalidUUID() {
        assertThatThrownBy(() -> leaveService.getEmployeeLeaveRequests(
                "not-a-uuid", Pageable.unpaged()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // approveLeaveRequest
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void approveLeaveRequest_setsApprovedStatus() {
        LeaveRequest request = buildPendingRequest();
        when(leaveRequestRepository.findById(leaveRequestId)).thenReturn(Optional.of(request));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveService.approveLeaveRequest(leaveRequestId, approverId);

        assertThat(result.getStatus()).isEqualTo(LeaveStatusEnum.APPROVED);
        assertThat(result.getApprovedBy()).isEqualTo(approverId.toString());
        assertThat(result.getApprovedAt()).isNotNull();
        verify(leaveRequestRepository).save(request);
    }

    @Test
    void approveLeaveRequest_throwsEntityNotFound_whenMissing() {
        when(leaveRequestRepository.findById(leaveRequestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.approveLeaveRequest(leaveRequestId, approverId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Leave request not found");
    }

    @Test
    void approveLeaveRequest_throwsIllegalState_whenAlreadyApproved() {
        LeaveRequest request = buildApprovedRequest();
        when(leaveRequestRepository.findById(leaveRequestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> leaveService.approveLeaveRequest(leaveRequestId, approverId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending");
    }

    @Test
    void approveLeaveRequest_throwsIllegalState_whenRejected() {
        LeaveRequest request = buildPendingRequest();
        request.setStatus(LeaveStatusEnum.REJECTED);
        when(leaveRequestRepository.findById(leaveRequestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> leaveService.approveLeaveRequest(leaveRequestId, approverId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending");
    }

    @Test
    void approveLeaveRequest_throwsIllegalState_whenCancelled() {
        LeaveRequest request = buildPendingRequest();
        request.setStatus(LeaveStatusEnum.CANCELLED);
        when(leaveRequestRepository.findById(leaveRequestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> leaveService.approveLeaveRequest(leaveRequestId, approverId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // rejectLeaveRequest
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void rejectLeaveRequest_setsRejectedStatusAndReason() {
        LeaveRequest request = buildPendingRequest();
        String rejectionReason = "Insufficient coverage during that period";
        when(leaveRequestRepository.findById(leaveRequestId)).thenReturn(Optional.of(request));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveService.rejectLeaveRequest(leaveRequestId, approverId, rejectionReason);

        assertThat(result.getStatus()).isEqualTo(LeaveStatusEnum.REJECTED);
        assertThat(result.getApprovedBy()).isEqualTo(approverId.toString());
        assertThat(result.getRejectionReason()).isEqualTo(rejectionReason);
        assertThat(result.getApprovedAt()).isNotNull();
        verify(leaveRequestRepository).save(request);
    }

    @Test
    void rejectLeaveRequest_throwsEntityNotFound_whenMissing() {
        when(leaveRequestRepository.findById(leaveRequestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.rejectLeaveRequest(leaveRequestId, approverId, "reason"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Leave request not found");
    }

    @Test
    void rejectLeaveRequest_throwsIllegalState_whenAlreadyApproved() {
        LeaveRequest request = buildApprovedRequest();
        when(leaveRequestRepository.findById(leaveRequestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> leaveService.rejectLeaveRequest(leaveRequestId, approverId, "reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending");
    }

    @Test
    void rejectLeaveRequest_throwsIllegalState_whenAlreadyRejected() {
        LeaveRequest request = buildPendingRequest();
        request.setStatus(LeaveStatusEnum.REJECTED);
        when(leaveRequestRepository.findById(leaveRequestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> leaveService.rejectLeaveRequest(leaveRequestId, approverId, "reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending");
    }

    @Test
    void rejectLeaveRequest_throwsIllegalState_whenCancelled() {
        LeaveRequest request = buildPendingRequest();
        request.setStatus(LeaveStatusEnum.CANCELLED);
        when(leaveRequestRepository.findById(leaveRequestId)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> leaveService.rejectLeaveRequest(leaveRequestId, approverId, "reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not pending");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getLeaveBalances
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getLeaveBalances_returnsList() {
        LeaveBalanceEntity vacBalance = buildLeaveBalance(LeaveTypeEnum.VACATION, 2025);
        LeaveBalanceEntity sickBalance = buildLeaveBalance(LeaveTypeEnum.SICK, 2025);
        when(leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, 2025))
                .thenReturn(List.of(vacBalance, sickBalance));

        List<LeaveBalanceEntity> result = leaveService.getLeaveBalances(employeeId.toString(), 2025);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(LeaveBalanceEntity::getLeaveType)
                .containsExactly(LeaveTypeEnum.VACATION, LeaveTypeEnum.SICK);
        verify(leaveBalanceRepository).findByEmployeeIdAndYear(employeeId, 2025);
    }

    @Test
    void getLeaveBalances_returnsEmptyList_whenNoBalances() {
        when(leaveBalanceRepository.findByEmployeeIdAndYear(employeeId, 2025))
                .thenReturn(Collections.emptyList());

        List<LeaveBalanceEntity> result = leaveService.getLeaveBalances(employeeId.toString(), 2025);

        assertThat(result).isEmpty();
    }

    @Test
    void getLeaveBalances_throwsException_whenInvalidUUID() {
        assertThatThrownBy(() -> leaveService.getLeaveBalances("bad-uuid", 2025))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getPendingLeaveRequests
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getPendingLeaveRequests_returnsList() {
        LeaveRequest r1 = buildPendingRequest();
        LeaveRequest r2 = buildPendingRequest();
        r2.setId(UUID.randomUUID());
        when(leaveRequestRepository.findByStatus(LeaveStatusEnum.PENDING))
                .thenReturn(List.of(r1, r2));

        List<LeaveRequest> result = leaveService.getPendingLeaveRequests();

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(r -> r.getStatus() == LeaveStatusEnum.PENDING);
        verify(leaveRequestRepository).findByStatus(LeaveStatusEnum.PENDING);
    }

    @Test
    void getPendingLeaveRequests_returnsEmptyList_whenNonePending() {
        when(leaveRequestRepository.findByStatus(LeaveStatusEnum.PENDING))
                .thenReturn(Collections.emptyList());

        List<LeaveRequest> result = leaveService.getPendingLeaveRequests();

        assertThat(result).isEmpty();
    }
}
