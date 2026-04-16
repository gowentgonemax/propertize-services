package com.propertize.payroll.service;

import com.propertize.payroll.dto.timeentry.TimeEntryDTO;
import com.propertize.payroll.dto.timesheet.response.TimesheetResponse;
import com.propertize.payroll.entity.TimesheetEntity;
import com.propertize.payroll.repository.TimesheetRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class TimesheetService {

    private final TimesheetRepository timesheetRepository;

    @Cacheable(value = "timesheets", key = "'id-' + #id")
    @Transactional(readOnly = true)
    public TimesheetResponse getTimesheet(UUID id) {
        TimesheetEntity timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet not found: " + id));
        return mapToResponse(timesheet);
    }

    @Transactional(readOnly = true)
    public Page<TimesheetResponse> getAllTimesheets(Pageable pageable) {
        return timesheetRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<TimesheetResponse> getEmployeeTimesheets(String employeeId, Pageable pageable) {
        return timesheetRepository.findByEmployeeId(employeeId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public TimesheetResponse submitTimesheet(UUID id) {
        TimesheetEntity timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet not found: " + id));

        timesheet.submit();
        timesheet = timesheetRepository.save(timesheet);

        log.info("Submitted timesheet: {}", id);
        return mapToResponse(timesheet);
    }

    @Transactional
    public TimesheetResponse approveTimesheet(UUID id, UUID approverId) {
        TimesheetEntity timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet not found: " + id));

        timesheet.approve(approverId.toString());
        timesheet = timesheetRepository.save(timesheet);

        log.info("Approved timesheet: {} by user: {}", id, approverId);
        return mapToResponse(timesheet);
    }

    @Transactional
    public TimesheetResponse rejectTimesheet(UUID id, UUID rejectorId, String reason) {
        TimesheetEntity timesheet = timesheetRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timesheet not found: " + id));

        timesheet.reject(rejectorId.toString(), reason);
        timesheet = timesheetRepository.save(timesheet);

        log.info("Rejected timesheet: {} by user: {}", id, rejectorId);
        return mapToResponse(timesheet);
    }

    @Transactional(readOnly = true)
    public List<TimesheetResponse> getPendingApprovals(UUID clientId) {
        return timesheetRepository.findPendingApprovalsForClient(clientId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Batch retrieval of timesheets for a single employee within a date range.
     * Avoids N calls for a date-range summary — returns all at once.
     */
    @Cacheable(value = "timesheets", key = "'range-' + #employeeId + '-' + #startDate + '-' + #endDate")
    @Transactional(readOnly = true)
    public List<TimesheetResponse> getTimesheetsByDateRange(String employeeId, LocalDate startDate, LocalDate endDate) {
        return timesheetRepository.findByEmployeeIdAndDateRange(employeeId, startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Batch retrieval of timesheets for multiple employees within a date range.
     * Use this instead of calling getTimesheetsByDateRange per-employee in a loop.
     */
    @Transactional(readOnly = true)
    public List<TimesheetResponse> getTimesheetsByEmployeesAndDateRange(
            List<String> employeeIds, LocalDate startDate, LocalDate endDate) {
        return timesheetRepository.findByEmployeeIdInAndDateRange(employeeIds, startDate, endDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TimesheetResponse mapToResponse(TimesheetEntity entity) {
        TimesheetResponse.TimesheetResponseBuilder builder = TimesheetResponse.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployeeId())
                .totalRegularHours(entity.getTotalRegularHours())
                .totalOvertimeHours(entity.getTotalOvertimeHours())
                .totalDoubleTimeHours(entity.getTotalDoubleTimeHours())
                .totalPtoHours(entity.getTotalPtoHours())
                .totalSickHours(entity.getTotalSickHours())
                .totalHolidayHours(entity.getTotalHolidayHours())
                .totalHours(entity.getTotalHours())
                .status(entity.getStatus())
                .submittedAt(entity.getSubmittedAt())
                .rejectionReason(entity.getRejectionReason())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .timeEntries(new ArrayList<>());

        if (entity.getPayPeriod() != null) {
            builder.payPeriodId(entity.getPayPeriod().getId());
        }

        if (entity.getWeekPeriod() != null) {
            builder.weekStartDate(entity.getWeekPeriod().getStartDate())
                    .weekEndDate(entity.getWeekPeriod().getEndDate());
        }

        if (entity.getApprovedBy() != null) {
            builder.approvedByName(entity.getApprovedBy())
                    .approvedAt(entity.getApprovedAt());
        }

        return builder.build();
    }

    public List<TimesheetResponse> getTimesheetsByDateRange(String employeeId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return timesheetRepository.findByEmployeeIdAndDateRange(employeeId, startDate, endDate)
                .stream().map(this::mapToResponse).toList();
    }

    public List<TimesheetResponse> getTimesheetsByEmployeesAndDateRange(List<String> employeeIds, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return timesheetRepository.findByEmployeeIdInAndDateRange(employeeIds, startDate, endDate)
                .stream().map(this::mapToResponse).toList();
    }
}
