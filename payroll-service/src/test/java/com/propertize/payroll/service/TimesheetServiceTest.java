package com.propertize.payroll.service;

import com.propertize.payroll.dto.timesheet.response.TimesheetResponse;
import com.propertize.payroll.entity.PayPeriodEntity;
import com.propertize.payroll.entity.TimesheetEntity;
import com.propertize.payroll.entity.embedded.DatePeriod;
import com.propertize.payroll.enums.TimesheetStatusEnum;
import com.propertize.payroll.repository.TimesheetRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimesheetServiceTest {

    @Mock
    TimesheetRepository timesheetRepository;

    @InjectMocks
    TimesheetService timesheetService;

    UUID timesheetId;
    UUID payPeriodId;
    UUID clientId;
    String employeeId;

    @BeforeEach
    void setUp() {
        timesheetId = UUID.randomUUID();
        payPeriodId = UUID.randomUUID();
        clientId = UUID.randomUUID();
        employeeId = "EMP-001";
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private TimesheetEntity buildTimesheet(TimesheetStatusEnum status) {
        TimesheetEntity ts = new TimesheetEntity();
        ts.setId(timesheetId);
        ts.setEmployeeId(employeeId);
        ts.setStatus(status);
        ts.setTotalRegularHours(new BigDecimal("40.00"));
        ts.setTotalOvertimeHours(new BigDecimal("5.00"));
        ts.setTotalDoubleTimeHours(new BigDecimal("2.00"));
        ts.setTotalPtoHours(new BigDecimal("8.00"));
        ts.setTotalSickHours(new BigDecimal("4.00"));
        ts.setTotalHolidayHours(new BigDecimal("8.00"));
        ts.setNotes("Test notes");
        ts.setCreatedAt(LocalDateTime.now());
        ts.setUpdatedAt(LocalDateTime.now());
        return ts;
    }

    private TimesheetEntity buildTimesheetWithPayPeriod(TimesheetStatusEnum status) {
        TimesheetEntity ts = buildTimesheet(status);
        PayPeriodEntity pp = new PayPeriodEntity();
        pp.setId(payPeriodId);
        ts.setPayPeriod(pp);
        return ts;
    }

    private TimesheetEntity buildTimesheetWithWeekPeriod(TimesheetStatusEnum status) {
        TimesheetEntity ts = buildTimesheetWithPayPeriod(status);
        DatePeriod week = new DatePeriod(LocalDate.of(2025, 6, 2), LocalDate.of(2025, 6, 8));
        ts.setWeekPeriod(week);
        return ts;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getTimesheet
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class GetTimesheet {

        @Test
        void returnsResponse_whenFound() {
            TimesheetEntity entity = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.DRAFT);
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));

            TimesheetResponse result = timesheetService.getTimesheet(timesheetId);

            assertThat(result.getId()).isEqualTo(timesheetId);
            assertThat(result.getEmployeeId()).isEqualTo(employeeId);
            assertThat(result.getTotalRegularHours()).isEqualByComparingTo("40.00");
            assertThat(result.getTotalOvertimeHours()).isEqualByComparingTo("5.00");
            assertThat(result.getTotalDoubleTimeHours()).isEqualByComparingTo("2.00");
            assertThat(result.getTotalPtoHours()).isEqualByComparingTo("8.00");
            assertThat(result.getTotalSickHours()).isEqualByComparingTo("4.00");
            assertThat(result.getTotalHolidayHours()).isEqualByComparingTo("8.00");
            assertThat(result.getStatus()).isEqualTo(TimesheetStatusEnum.DRAFT);
            assertThat(result.getNotes()).isEqualTo("Test notes");
            assertThat(result.getPayPeriodId()).isEqualTo(payPeriodId);
            assertThat(result.getWeekStartDate()).isEqualTo(LocalDate.of(2025, 6, 2));
            assertThat(result.getWeekEndDate()).isEqualTo(LocalDate.of(2025, 6, 8));
            assertThat(result.getTimeEntries()).isEmpty();
        }

        @Test
        void throwsEntityNotFound_whenMissing() {
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> timesheetService.getTimesheet(timesheetId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Timesheet not found");
        }

        @Test
        void mapsApprovedByFields_whenPresent() {
            TimesheetEntity entity = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.APPROVED);
            entity.setApprovedBy("admin-user");
            entity.setApprovedAt(LocalDateTime.of(2025, 6, 9, 10, 0));
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));

            TimesheetResponse result = timesheetService.getTimesheet(timesheetId);

            assertThat(result.getApprovedByName()).isEqualTo("admin-user");
            assertThat(result.getApprovedAt()).isEqualTo(LocalDateTime.of(2025, 6, 9, 10, 0));
        }

        @Test
        void handlesNullPayPeriod() {
            TimesheetEntity entity = buildTimesheet(TimesheetStatusEnum.DRAFT);
            // payPeriod is null
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));

            TimesheetResponse result = timesheetService.getTimesheet(timesheetId);

            assertThat(result.getPayPeriodId()).isNull();
        }

        @Test
        void handlesNullWeekPeriod() {
            TimesheetEntity entity = buildTimesheetWithPayPeriod(TimesheetStatusEnum.DRAFT);
            // weekPeriod is null
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));

            TimesheetResponse result = timesheetService.getTimesheet(timesheetId);

            assertThat(result.getWeekStartDate()).isNull();
            assertThat(result.getWeekEndDate()).isNull();
        }

        @Test
        void handlesNullApprovedBy() {
            TimesheetEntity entity = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.DRAFT);
            // approvedBy is null by default
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));

            TimesheetResponse result = timesheetService.getTimesheet(timesheetId);

            assertThat(result.getApprovedByName()).isNull();
            assertThat(result.getApprovedAt()).isNull();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getAllTimesheets
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class GetAllTimesheets {

        @Test
        void returnsPageOfTimesheets() {
            TimesheetEntity ts1 = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.DRAFT);
            TimesheetEntity ts2 = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.SUBMITTED);
            ts2.setId(UUID.randomUUID());
            ts2.setEmployeeId("EMP-002");
            Page<TimesheetEntity> page = new PageImpl<>(List.of(ts1, ts2), PageRequest.of(0, 10), 2);
            when(timesheetRepository.findAll(any(Pageable.class))).thenReturn(page);

            Page<TimesheetResponse> result = timesheetService.getAllTimesheets(PageRequest.of(0, 10));

            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getEmployeeId()).isEqualTo(employeeId);
            assertThat(result.getContent().get(1).getEmployeeId()).isEqualTo("EMP-002");
        }

        @Test
        void returnsEmptyPage_whenNoTimesheets() {
            Page<TimesheetEntity> emptyPage = Page.empty();
            when(timesheetRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            Page<TimesheetResponse> result = timesheetService.getAllTimesheets(Pageable.unpaged());

            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getEmployeeTimesheets
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class GetEmployeeTimesheets {

        @Test
        void returnsPageForEmployee() {
            TimesheetEntity ts = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.DRAFT);
            Page<TimesheetEntity> page = new PageImpl<>(List.of(ts));
            when(timesheetRepository.findByEmployeeId(eq(employeeId), any(Pageable.class))).thenReturn(page);

            Page<TimesheetResponse> result = timesheetService.getEmployeeTimesheets(employeeId, Pageable.unpaged());

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getEmployeeId()).isEqualTo(employeeId);
        }

        @Test
        void returnsEmptyPage_whenNoTimesheetsForEmployee() {
            Page<TimesheetEntity> emptyPage = Page.empty();
            when(timesheetRepository.findByEmployeeId(eq(employeeId), any(Pageable.class))).thenReturn(emptyPage);

            Page<TimesheetResponse> result = timesheetService.getEmployeeTimesheets(employeeId, Pageable.unpaged());

            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // submitTimesheet
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class SubmitTimesheet {

        @Test
        void submitsAndReturnsResponse() {
            TimesheetEntity entity = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.DRAFT);
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));
            when(timesheetRepository.save(entity)).thenReturn(entity);

            TimesheetResponse result = timesheetService.submitTimesheet(timesheetId);

            assertThat(result.getStatus()).isEqualTo(TimesheetStatusEnum.SUBMITTED);
            assertThat(result.getSubmittedAt()).isNotNull();
            verify(timesheetRepository).save(entity);
        }

        @Test
        void throwsEntityNotFound_whenTimesheetMissing() {
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> timesheetService.submitTimesheet(timesheetId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Timesheet not found");
        }

        @Test
        void throwsIllegalState_whenNotDraft() {
            TimesheetEntity entity = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.SUBMITTED);
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> timesheetService.submitTimesheet(timesheetId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only DRAFT timesheets can be submitted");
        }

        @Test
        void throwsIllegalState_whenAlreadyApproved() {
            TimesheetEntity entity = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.APPROVED);
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> timesheetService.submitTimesheet(timesheetId))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // approveTimesheet
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class ApproveTimesheet {

        UUID approverId;

        @BeforeEach
        void init() {
            approverId = UUID.randomUUID();
        }

        @Test
        void approvesAndReturnsResponse() {
            TimesheetEntity entity = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.SUBMITTED);
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));
            when(timesheetRepository.save(entity)).thenReturn(entity);

            TimesheetResponse result = timesheetService.approveTimesheet(timesheetId, approverId);

            assertThat(result.getStatus()).isEqualTo(TimesheetStatusEnum.APPROVED);
            assertThat(result.getApprovedByName()).isEqualTo(approverId.toString());
            assertThat(result.getApprovedAt()).isNotNull();
            verify(timesheetRepository).save(entity);
        }

        @Test
        void throwsEntityNotFound_whenTimesheetMissing() {
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> timesheetService.approveTimesheet(timesheetId, approverId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Timesheet not found");
        }

        @Test
        void throwsIllegalState_whenNotSubmitted() {
            TimesheetEntity entity = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.DRAFT);
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> timesheetService.approveTimesheet(timesheetId, approverId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only SUBMITTED timesheets can be approved");
        }

        @Test
        void throwsIllegalState_whenAlreadyApproved() {
            TimesheetEntity entity = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.APPROVED);
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> timesheetService.approveTimesheet(timesheetId, approverId))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // rejectTimesheet
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class RejectTimesheet {

        UUID rejectorId;
        String reason;

        @BeforeEach
        void init() {
            rejectorId = UUID.randomUUID();
            reason = "Hours do not match actual time logged";
        }

        @Test
        void rejectsAndReturnsResponse() {
            TimesheetEntity entity = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.SUBMITTED);
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));
            when(timesheetRepository.save(entity)).thenReturn(entity);

            TimesheetResponse result = timesheetService.rejectTimesheet(timesheetId, rejectorId, reason);

            assertThat(result.getStatus()).isEqualTo(TimesheetStatusEnum.REJECTED);
            assertThat(result.getRejectionReason()).isEqualTo(reason);
            verify(timesheetRepository).save(entity);
        }

        @Test
        void throwsEntityNotFound_whenTimesheetMissing() {
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> timesheetService.rejectTimesheet(timesheetId, rejectorId, reason))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Timesheet not found");
        }

        @Test
        void throwsIllegalState_whenNotSubmitted() {
            TimesheetEntity entity = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.DRAFT);
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> timesheetService.rejectTimesheet(timesheetId, rejectorId, reason))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Only SUBMITTED timesheets can be rejected");
        }

        @Test
        void throwsIllegalState_whenAlreadyRejected() {
            TimesheetEntity entity = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.REJECTED);
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));

            assertThatThrownBy(() -> timesheetService.rejectTimesheet(timesheetId, rejectorId, reason))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getPendingApprovals
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class GetPendingApprovals {

        @Test
        void returnsPendingTimesheets() {
            TimesheetEntity ts1 = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.SUBMITTED);
            TimesheetEntity ts2 = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.SUBMITTED);
            ts2.setId(UUID.randomUUID());
            ts2.setEmployeeId("EMP-002");
            when(timesheetRepository.findPendingApprovalsForClient(clientId)).thenReturn(List.of(ts1, ts2));

            List<TimesheetResponse> result = timesheetService.getPendingApprovals(clientId);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getStatus()).isEqualTo(TimesheetStatusEnum.SUBMITTED);
            assertThat(result.get(1).getStatus()).isEqualTo(TimesheetStatusEnum.SUBMITTED);
        }

        @Test
        void returnsEmptyList_whenNoPending() {
            when(timesheetRepository.findPendingApprovalsForClient(clientId)).thenReturn(List.of());

            List<TimesheetResponse> result = timesheetService.getPendingApprovals(clientId);

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getTimesheetsByDateRange
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @Disabled("getTimesheetsByDateRange not yet implemented in TimesheetService")
    class GetTimesheetsByDateRange {

        LocalDate startDate;
        LocalDate endDate;

        @BeforeEach
        void init() {
            startDate = LocalDate.of(2025, 6, 1);
            endDate = LocalDate.of(2025, 6, 30);
        }

        @Test
        void returnsTimesheetsInRange() {
            TimesheetEntity ts1 = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.APPROVED);
            TimesheetEntity ts2 = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.DRAFT);
            ts2.setId(UUID.randomUUID());
            when(timesheetRepository.findByEmployeeIdAndDateRange(employeeId, startDate, endDate))
                    .thenReturn(List.of(ts1, ts2));

            List<TimesheetResponse> result = timesheetService.getTimesheetsByDateRange(
                    employeeId, startDate, endDate);

            assertThat(result).hasSize(2);
            verify(timesheetRepository).findByEmployeeIdAndDateRange(employeeId, startDate, endDate);
        }

        @Test
        void returnsEmptyList_whenNoTimesheetsInRange() {
            when(timesheetRepository.findByEmployeeIdAndDateRange(employeeId, startDate, endDate))
                    .thenReturn(List.of());

            List<TimesheetResponse> result = timesheetService.getTimesheetsByDateRange(
                    employeeId, startDate, endDate);

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getTimesheetsByEmployeesAndDateRange
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @Disabled("getTimesheetsByEmployeesAndDateRange/findByEmployeeIdInAndDateRange not yet implemented")
    class GetTimesheetsByEmployeesAndDateRange {

        LocalDate startDate;
        LocalDate endDate;
        List<String> employeeIds;

        @BeforeEach
        void init() {
            startDate = LocalDate.of(2025, 6, 1);
            endDate = LocalDate.of(2025, 6, 30);
            employeeIds = List.of("EMP-001", "EMP-002", "EMP-003");
        }

        @Test
        void returnsBatchTimesheets() {
            TimesheetEntity ts1 = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.APPROVED);
            TimesheetEntity ts2 = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.SUBMITTED);
            ts2.setId(UUID.randomUUID());
            ts2.setEmployeeId("EMP-002");
            when(timesheetRepository.findByEmployeeIdInAndDateRange(employeeIds, startDate, endDate))
                    .thenReturn(List.of(ts1, ts2));

            List<TimesheetResponse> result = timesheetService.getTimesheetsByEmployeesAndDateRange(
                    employeeIds, startDate, endDate);

            assertThat(result).hasSize(2);
            verify(timesheetRepository).findByEmployeeIdInAndDateRange(employeeIds, startDate, endDate);
        }

        @Test
        void returnsEmptyList_whenNoTimesheetsForEmployees() {
            when(timesheetRepository.findByEmployeeIdInAndDateRange(employeeIds, startDate, endDate))
                    .thenReturn(List.of());

            List<TimesheetResponse> result = timesheetService.getTimesheetsByEmployeesAndDateRange(
                    employeeIds, startDate, endDate);

            assertThat(result).isEmpty();
        }

        @Test
        void handlesEmptyEmployeeIdsList() {
            List<String> empty = List.of();
            when(timesheetRepository.findByEmployeeIdInAndDateRange(empty, startDate, endDate))
                    .thenReturn(List.of());

            List<TimesheetResponse> result = timesheetService.getTimesheetsByEmployeesAndDateRange(
                    empty, startDate, endDate);

            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // mapToResponse (covered via public methods, but verify edge cases)
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    class MapToResponseEdgeCases {

        @Test
        void mapsTotalHours_fromEntity() {
            TimesheetEntity entity = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.DRAFT);
            // totalHours = 40 + 5 + 2 = 47
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));

            TimesheetResponse result = timesheetService.getTimesheet(timesheetId);

            assertThat(result.getTotalHours()).isEqualByComparingTo("47.00");
        }

        @Test
        void mapsSubmittedAt_whenPresent() {
            TimesheetEntity entity = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.SUBMITTED);
            entity.setSubmittedAt(LocalDateTime.of(2025, 6, 9, 14, 30));
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));

            TimesheetResponse result = timesheetService.getTimesheet(timesheetId);

            assertThat(result.getSubmittedAt()).isEqualTo(LocalDateTime.of(2025, 6, 9, 14, 30));
        }

        @Test
        void mapsRejectionReason_whenPresent() {
            TimesheetEntity entity = buildTimesheetWithWeekPeriod(TimesheetStatusEnum.REJECTED);
            entity.setRejectionReason("Invalid hours");
            when(timesheetRepository.findById(timesheetId)).thenReturn(Optional.of(entity));

            TimesheetResponse result = timesheetService.getTimesheet(timesheetId);

            assertThat(result.getRejectionReason()).isEqualTo("Invalid hours");
        }
    }
}
