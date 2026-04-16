package com.propertize.payroll.service;

import com.propertize.payroll.dto.timeentry.CreateTimeEntryRequest;
import com.propertize.payroll.dto.timeentry.TimeEntryDTO;
import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.payroll.entity.TimeEntry;
import com.propertize.payroll.enums.TimeEntryStatusEnum;
import com.propertize.payroll.repository.EmployeeEntityRepository;
import com.propertize.payroll.repository.TimeEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeEntryServiceTest {

    @Mock
    TimeEntryRepository timeEntryRepository;
    @Mock
    EmployeeEntityRepository employeeRepository;

    @InjectMocks
    TimeEntryService timeEntryService;

    UUID employeeId;
    UUID entryId;
    UUID clientId;
    EmployeeEntity employee;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        entryId = UUID.randomUUID();
        clientId = UUID.randomUUID();

        employee = new EmployeeEntity();
        employee.setId(employeeId);
        employee.setFirstName("Jane");
        employee.setLastName("Doe");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private TimeEntry buildPendingEntry() {
        TimeEntry entry = new TimeEntry();
        entry.setId(entryId);
        entry.setEmployee(employee);
        entry.setWorkDate(LocalDate.of(2025, 6, 1));
        entry.setRegularHours(new BigDecimal("8.00"));
        entry.setOvertimeHours(BigDecimal.ZERO);
        entry.setDoubleTimeHours(BigDecimal.ZERO);
        entry.setBreakMinutes(30);
        entry.setStatus(TimeEntryStatusEnum.PENDING);
        entry.setNotes("Standard shift");
        entry.setDepartment("Maintenance");
        entry.setProjectCode("PROJ-01");
        entry.setCostCenter("CC-100");
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());
        return entry;
    }

    private CreateTimeEntryRequest buildCreateRequest() {
        return CreateTimeEntryRequest.builder()
                .employeeId(employeeId)
                .workDate(LocalDate.of(2025, 6, 1))
                .clockIn(LocalTime.of(9, 0))
                .clockOut(LocalTime.of(17, 30))
                .regularHours(new BigDecimal("8.00"))
                .overtimeHours(new BigDecimal("0.50"))
                .doubleTimeHours(BigDecimal.ZERO)
                .breakMinutes(30)
                .notes("Standard shift")
                .department("Maintenance")
                .projectCode("PROJ-01")
                .costCenter("CC-100")
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // createTimeEntry
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void createTimeEntry_happy_path() {
        CreateTimeEntryRequest request = buildCreateRequest();
        TimeEntry savedEntry = buildPendingEntry();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(savedEntry);

        TimeEntryDTO result = timeEntryService.createTimeEntry(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(entryId);
        assertThat(result.getEmployeeId()).isEqualTo(employeeId);
        assertThat(result.getStatus()).isEqualTo(TimeEntryStatusEnum.PENDING);
        assertThat(result.getRegularHours()).isEqualByComparingTo("8.00");
        verify(timeEntryRepository).save(any(TimeEntry.class));
    }

    @Test
    void createTimeEntry_setsStatusToPending() {
        CreateTimeEntryRequest request = buildCreateRequest();
        TimeEntry savedEntry = buildPendingEntry();

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(savedEntry);

        timeEntryService.createTimeEntry(request);

        ArgumentCaptor<TimeEntry> captor = ArgumentCaptor.forClass(TimeEntry.class);
        verify(timeEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TimeEntryStatusEnum.PENDING);
    }

    @Test
    void createTimeEntry_nullHoursDefaultToZero() {
        CreateTimeEntryRequest request = CreateTimeEntryRequest.builder()
                .employeeId(employeeId)
                .workDate(LocalDate.of(2025, 6, 1))
                .build();
        TimeEntry savedEntry = buildPendingEntry();
        savedEntry.setRegularHours(BigDecimal.ZERO);
        savedEntry.setOvertimeHours(BigDecimal.ZERO);
        savedEntry.setDoubleTimeHours(BigDecimal.ZERO);
        savedEntry.setBreakMinutes(0);

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(savedEntry);

        TimeEntryDTO result = timeEntryService.createTimeEntry(request);

        ArgumentCaptor<TimeEntry> captor = ArgumentCaptor.forClass(TimeEntry.class);
        verify(timeEntryRepository).save(captor.capture());
        TimeEntry saved = captor.getValue();
        assertThat(saved.getRegularHours()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getOvertimeHours()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getDoubleTimeHours()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getBreakMinutes()).isEqualTo(0);
    }

    @Test
    void createTimeEntry_employeeNotFound_throws() {
        CreateTimeEntryRequest request = buildCreateRequest();
        when(employeeRepository.findById(employeeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeEntryService.createTimeEntry(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(employeeId.toString());

        verify(timeEntryRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getTimeEntryById
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getTimeEntryById_returnsDTO() {
        TimeEntry entry = buildPendingEntry();
        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        TimeEntryDTO result = timeEntryService.getTimeEntryById(entryId);

        assertThat(result.getId()).isEqualTo(entryId);
        assertThat(result.getEmployeeId()).isEqualTo(employeeId);
        assertThat(result.getWorkDate()).isEqualTo(LocalDate.of(2025, 6, 1));
    }

    @Test
    void getTimeEntryById_notFound_throws() {
        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeEntryService.getTimeEntryById(entryId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(entryId.toString());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getTimeEntriesByEmployee
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getTimeEntriesByEmployee_returnsPage() {
        TimeEntry entry = buildPendingEntry();
        Page<TimeEntry> page = new PageImpl<>(List.of(entry));
        Pageable pageable = PageRequest.of(0, 10);

        when(timeEntryRepository.findByEmployeeId(eq(employeeId), any(Pageable.class))).thenReturn(page);

        Page<TimeEntryDTO> result = timeEntryService.getTimeEntriesByEmployee(employeeId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getEmployeeId()).isEqualTo(employeeId);
    }

    @Test
    void getTimeEntriesByEmployee_emptyPage_returnsEmpty() {
        Page<TimeEntry> emptyPage = new PageImpl<>(List.of());
        when(timeEntryRepository.findByEmployeeId(eq(employeeId), any(Pageable.class))).thenReturn(emptyPage);

        Page<TimeEntryDTO> result = timeEntryService.getTimeEntriesByEmployee(employeeId, PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getTimeEntriesByEmployeeAndDateRange
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getTimeEntriesByEmployeeAndDateRange_returnsList() {
        TimeEntry entry = buildPendingEntry();
        LocalDate start = LocalDate.of(2025, 6, 1);
        LocalDate end = LocalDate.of(2025, 6, 30);

        when(timeEntryRepository.findByEmployeeIdAndDateRange(employeeId, start, end))
                .thenReturn(List.of(entry));

        List<TimeEntryDTO> result = timeEntryService.getTimeEntriesByEmployeeAndDateRange(employeeId, start, end);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getWorkDate()).isEqualTo(LocalDate.of(2025, 6, 1));
    }

    @Test
    void getTimeEntriesByEmployeeAndDateRange_noResults_returnsEmptyList() {
        LocalDate start = LocalDate.of(2025, 6, 1);
        LocalDate end = LocalDate.of(2025, 6, 30);

        when(timeEntryRepository.findByEmployeeIdAndDateRange(employeeId, start, end))
                .thenReturn(List.of());

        List<TimeEntryDTO> result = timeEntryService.getTimeEntriesByEmployeeAndDateRange(employeeId, start, end);

        assertThat(result).isEmpty();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getTimeEntriesByClientAndDateRange
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getTimeEntriesByClientAndDateRange_returnsList() {
        TimeEntry entry = buildPendingEntry();
        LocalDate start = LocalDate.of(2025, 6, 1);
        LocalDate end = LocalDate.of(2025, 6, 30);

        when(timeEntryRepository.findByClientIdAndDateRange(clientId, start, end))
                .thenReturn(List.of(entry));

        List<TimeEntryDTO> result = timeEntryService.getTimeEntriesByClientAndDateRange(clientId, start, end);

        assertThat(result).hasSize(1);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getPendingTimeEntriesByClient
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getPendingTimeEntriesByClient_returnsPage() {
        TimeEntry entry = buildPendingEntry();
        Page<TimeEntry> page = new PageImpl<>(List.of(entry));
        Pageable pageable = PageRequest.of(0, 10);

        when(timeEntryRepository.findByClientIdAndStatus(
                eq(clientId), eq(TimeEntryStatusEnum.PENDING), any(Pageable.class)))
                .thenReturn(page);

        Page<TimeEntryDTO> result = timeEntryService.getPendingTimeEntriesByClient(clientId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(TimeEntryStatusEnum.PENDING);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // approveTimeEntry
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void approveTimeEntry_pendingEntry_approvesSuccessfully() {
        UUID approverId = UUID.randomUUID();
        TimeEntry entry = buildPendingEntry();
        TimeEntry approvedEntry = buildPendingEntry();
        approvedEntry.setStatus(TimeEntryStatusEnum.APPROVED);
        approvedEntry.setApprovedBy(approverId.toString());

        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(approvedEntry);

        TimeEntryDTO result = timeEntryService.approveTimeEntry(entryId, approverId);

        assertThat(result.getStatus()).isEqualTo(TimeEntryStatusEnum.APPROVED);
        verify(timeEntryRepository).save(entry);
    }

    @Test
    void approveTimeEntry_notFound_throws() {
        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeEntryService.approveTimeEntry(entryId, UUID.randomUUID()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(entryId.toString());
    }

    @Test
    void approveTimeEntry_alreadyApproved_throws() {
        TimeEntry entry = buildPendingEntry();
        entry.setStatus(TimeEntryStatusEnum.APPROVED);

        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> timeEntryService.approveTimeEntry(entryId, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending time entries can be approved");

        verify(timeEntryRepository, never()).save(any());
    }

    @Test
    void approveTimeEntry_rejectedEntry_throws() {
        TimeEntry entry = buildPendingEntry();
        entry.setStatus(TimeEntryStatusEnum.REJECTED);

        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> timeEntryService.approveTimeEntry(entryId, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // rejectTimeEntry
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void rejectTimeEntry_pendingEntry_rejectsSuccessfully() {
        TimeEntry entry = buildPendingEntry();
        TimeEntry rejectedEntry = buildPendingEntry();
        rejectedEntry.setStatus(TimeEntryStatusEnum.REJECTED);
        rejectedEntry.setNotes("Bad data");

        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(rejectedEntry);

        TimeEntryDTO result = timeEntryService.rejectTimeEntry(entryId, "Bad data");

        assertThat(result.getStatus()).isEqualTo(TimeEntryStatusEnum.REJECTED);
        verify(timeEntryRepository).save(entry);
    }

    @Test
    void rejectTimeEntry_notFound_throws() {
        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeEntryService.rejectTimeEntry(entryId, "reason"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(entryId.toString());
    }

    @Test
    void rejectTimeEntry_alreadyApproved_throws() {
        TimeEntry entry = buildPendingEntry();
        entry.setStatus(TimeEntryStatusEnum.APPROVED);

        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> timeEntryService.rejectTimeEntry(entryId, "reason"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only pending time entries can be rejected");
    }

    @Test
    void rejectTimeEntry_setsNotesOnEntry() {
        TimeEntry entry = buildPendingEntry();
        TimeEntry rejectedEntry = buildPendingEntry();
        rejectedEntry.setStatus(TimeEntryStatusEnum.REJECTED);
        rejectedEntry.setNotes("Clock-in time invalid");

        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(rejectedEntry);

        timeEntryService.rejectTimeEntry(entryId, "Clock-in time invalid");

        ArgumentCaptor<TimeEntry> captor = ArgumentCaptor.forClass(TimeEntry.class);
        verify(timeEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TimeEntryStatusEnum.REJECTED);
        assertThat(captor.getValue().getNotes()).isEqualTo("Clock-in time invalid");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // deleteTimeEntry
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void deleteTimeEntry_pendingEntry_deletesSuccessfully() {
        TimeEntry entry = buildPendingEntry();
        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        timeEntryService.deleteTimeEntry(entryId);

        verify(timeEntryRepository).delete(entry);
    }

    @Test
    void deleteTimeEntry_rejectedEntry_deletesSuccessfully() {
        TimeEntry entry = buildPendingEntry();
        entry.setStatus(TimeEntryStatusEnum.REJECTED);
        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        timeEntryService.deleteTimeEntry(entryId);

        verify(timeEntryRepository).delete(entry);
    }

    @Test
    void deleteTimeEntry_approvedEntry_throws() {
        TimeEntry entry = buildPendingEntry();
        entry.setStatus(TimeEntryStatusEnum.APPROVED);

        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.of(entry));

        assertThatThrownBy(() -> timeEntryService.deleteTimeEntry(entryId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Approved time entries cannot be deleted");

        verify(timeEntryRepository, never()).delete(any());
    }

    @Test
    void deleteTimeEntry_notFound_throws() {
        when(timeEntryRepository.findById(entryId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> timeEntryService.deleteTimeEntry(entryId))
                .isInstanceOf(EntityNotFoundException.class);

        verify(timeEntryRepository, never()).delete(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getTotalApprovedRegularHours
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getTotalApprovedRegularHours_returnsSum() {
        LocalDate start = LocalDate.of(2025, 6, 1);
        LocalDate end = LocalDate.of(2025, 6, 30);

        when(timeEntryRepository.sumApprovedRegularHours(employeeId, start, end))
                .thenReturn(new BigDecimal("40.00"));

        BigDecimal result = timeEntryService.getTotalApprovedRegularHours(employeeId, start, end);

        assertThat(result).isEqualByComparingTo("40.00");
    }

    @Test
    void getTotalApprovedRegularHours_nullRepositoryResult_returnsZero() {
        LocalDate start = LocalDate.of(2025, 6, 1);
        LocalDate end = LocalDate.of(2025, 6, 30);

        when(timeEntryRepository.sumApprovedRegularHours(employeeId, start, end)).thenReturn(null);

        BigDecimal result = timeEntryService.getTotalApprovedRegularHours(employeeId, start, end);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // getTotalApprovedOvertimeHours
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void getTotalApprovedOvertimeHours_returnsSum() {
        LocalDate start = LocalDate.of(2025, 6, 1);
        LocalDate end = LocalDate.of(2025, 6, 30);

        when(timeEntryRepository.sumApprovedOvertimeHours(employeeId, start, end))
                .thenReturn(new BigDecimal("5.50"));

        BigDecimal result = timeEntryService.getTotalApprovedOvertimeHours(employeeId, start, end);

        assertThat(result).isEqualByComparingTo("5.50");
    }

    @Test
    void getTotalApprovedOvertimeHours_nullRepositoryResult_returnsZero() {
        LocalDate start = LocalDate.of(2025, 6, 1);
        LocalDate end = LocalDate.of(2025, 6, 30);

        when(timeEntryRepository.sumApprovedOvertimeHours(employeeId, start, end)).thenReturn(null);

        BigDecimal result = timeEntryService.getTotalApprovedOvertimeHours(employeeId, start, end);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DTO mapping
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void createTimeEntry_dtoMapsFieldsCorrectly() {
        CreateTimeEntryRequest request = buildCreateRequest();
        TimeEntry savedEntry = buildPendingEntry();
        savedEntry.setClockIn(LocalTime.of(9, 0));
        savedEntry.setClockOut(LocalTime.of(17, 30));
        savedEntry.setApprovedBy("approver-uuid");
        savedEntry.setApprovedAt(LocalDateTime.of(2025, 6, 2, 10, 0));

        when(employeeRepository.findById(employeeId)).thenReturn(Optional.of(employee));
        when(timeEntryRepository.save(any(TimeEntry.class))).thenReturn(savedEntry);

        TimeEntryDTO result = timeEntryService.createTimeEntry(request);

        assertThat(result.getEmployeeName()).isEqualTo("Jane Doe");
        assertThat(result.getDepartment()).isEqualTo("Maintenance");
        assertThat(result.getProjectCode()).isEqualTo("PROJ-01");
        assertThat(result.getCostCenter()).isEqualTo("CC-100");
        assertThat(result.getApprovedByName()).isEqualTo("approver-uuid");
    }
}
