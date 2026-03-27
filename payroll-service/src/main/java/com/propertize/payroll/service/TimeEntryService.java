package com.propertize.payroll.service;

import com.propertize.payroll.dto.timeentry.CreateTimeEntryRequest;
import com.propertize.payroll.dto.timeentry.TimeEntryDTO;
import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.payroll.entity.TimeEntry;
import com.propertize.payroll.enums.TimeEntryStatusEnum;
import com.propertize.payroll.repository.EmployeeEntityRepository;
import com.propertize.payroll.repository.TimeEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final EmployeeEntityRepository employeeRepository;

    public TimeEntryDTO createTimeEntry(CreateTimeEntryRequest request) {
        log.info("Creating time entry for employee: {} on date: {}", request.getEmployeeId(), request.getWorkDate());

        EmployeeEntity employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + request.getEmployeeId()));

        TimeEntry entry = new TimeEntry();
        entry.setEmployee(employee);
        entry.setWorkDate(request.getWorkDate());
        entry.setClockIn(request.getClockIn());
        entry.setClockOut(request.getClockOut());
        entry.setRegularHours(request.getRegularHours() != null ? request.getRegularHours() : BigDecimal.ZERO);
        entry.setOvertimeHours(request.getOvertimeHours() != null ? request.getOvertimeHours() : BigDecimal.ZERO);
        entry.setDoubleTimeHours(request.getDoubleTimeHours() != null ? request.getDoubleTimeHours() : BigDecimal.ZERO);
        entry.setBreakMinutes(request.getBreakMinutes() != null ? request.getBreakMinutes() : 0);
        entry.setNotes(request.getNotes());
        entry.setDepartment(request.getDepartment());
        entry.setProjectCode(request.getProjectCode());
        entry.setCostCenter(request.getCostCenter());
        entry.setStatus(TimeEntryStatusEnum.PENDING);

        TimeEntry saved = timeEntryRepository.save(entry);
        log.info("Time entry created with ID: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public TimeEntryDTO getTimeEntryById(UUID id) {
        TimeEntry entry = timeEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Time entry not found: " + id));
        return toDTO(entry);
    }

    @Transactional(readOnly = true)
    public Page<TimeEntryDTO> getTimeEntriesByEmployee(UUID employeeId, Pageable pageable) {
        return timeEntryRepository.findByEmployeeId(employeeId, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<TimeEntryDTO> getTimeEntriesByEmployeeAndDateRange(UUID employeeId, LocalDate startDate,
            LocalDate endDate) {
        return timeEntryRepository.findByEmployeeIdAndDateRange(employeeId, startDate, endDate)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TimeEntryDTO> getTimeEntriesByClientAndDateRange(UUID clientId, LocalDate startDate,
            LocalDate endDate) {
        return timeEntryRepository.findByClientIdAndDateRange(clientId, startDate, endDate)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TimeEntryDTO> getPendingTimeEntriesByClient(UUID clientId, Pageable pageable) {
        return timeEntryRepository.findByClientIdAndStatus(clientId, TimeEntryStatusEnum.PENDING, pageable)
                .map(this::toDTO);
    }

    public TimeEntryDTO approveTimeEntry(UUID id, UUID approverId) {
        log.info("Approving time entry: {} by user: {}", id, approverId);

        TimeEntry entry = timeEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Time entry not found: " + id));

        if (entry.getStatus() != TimeEntryStatusEnum.PENDING) {
            throw new IllegalStateException("Only pending time entries can be approved");
        }

        entry.approve(approverId.toString());

        TimeEntry saved = timeEntryRepository.save(entry);
        return toDTO(saved);
    }

    public TimeEntryDTO rejectTimeEntry(UUID id, String rejectionNotes) {
        log.info("Rejecting time entry: {}", id);

        TimeEntry entry = timeEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Time entry not found: " + id));

        if (entry.getStatus() != TimeEntryStatusEnum.PENDING) {
            throw new IllegalStateException("Only pending time entries can be rejected");
        }

        entry.setStatus(TimeEntryStatusEnum.REJECTED);
        entry.setNotes(rejectionNotes);

        TimeEntry saved = timeEntryRepository.save(entry);
        return toDTO(saved);
    }

    public void deleteTimeEntry(UUID id) {
        log.info("Deleting time entry: {}", id);

        TimeEntry entry = timeEntryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Time entry not found: " + id));

        if (entry.getStatus() == TimeEntryStatusEnum.APPROVED) {
            throw new IllegalStateException("Approved time entries cannot be deleted");
        }

        timeEntryRepository.delete(entry);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalApprovedRegularHours(UUID employeeId, LocalDate startDate, LocalDate endDate) {
        BigDecimal total = timeEntryRepository.sumApprovedRegularHours(employeeId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalApprovedOvertimeHours(UUID employeeId, LocalDate startDate, LocalDate endDate) {
        BigDecimal total = timeEntryRepository.sumApprovedOvertimeHours(employeeId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }

    private TimeEntryDTO toDTO(TimeEntry entity) {
        TimeEntryDTO dto = new TimeEntryDTO();
        dto.setId(entity.getId());
        dto.setEmployeeId(entity.getEmployee().getId());
        dto.setEmployeeName(entity.getEmployee().getFullName());
        dto.setWorkDate(entity.getWorkDate());
        dto.setClockIn(entity.getClockIn());
        dto.setClockOut(entity.getClockOut());
        dto.setRegularHours(entity.getRegularHours());
        dto.setOvertimeHours(entity.getOvertimeHours());
        dto.setDoubleTimeHours(entity.getDoubleTimeHours());
        dto.setTotalHours(entity.calculateTotalHours());
        dto.setBreakMinutes(entity.getBreakMinutes());
        dto.setStatus(entity.getStatus());
        dto.setNotes(entity.getNotes());
        dto.setDepartment(entity.getDepartment());
        dto.setProjectCode(entity.getProjectCode());
        dto.setCostCenter(entity.getCostCenter());

        if (entity.getApprovedBy() != null) {
            dto.setApprovedByName(entity.getApprovedBy());
        }
        dto.setApprovedAt(entity.getApprovedAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }
}
