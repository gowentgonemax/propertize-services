package com.propertize.payroll.service;

import com.propertize.payroll.entity.LeaveBalanceEntity;
import com.propertize.payroll.entity.LeaveRequest;
import com.propertize.payroll.enums.LeaveStatusEnum;
import com.propertize.payroll.repository.LeaveBalanceRepository;
import com.propertize.payroll.repository.LeaveRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    @Transactional(readOnly = true)
    public LeaveRequest getLeaveRequest(UUID id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<LeaveRequest> getEmployeeLeaveRequests(String employeeId, Pageable pageable) {
        return leaveRequestRepository.findByEmployeeId(UUID.fromString(employeeId), pageable);
    }

    @Transactional
    public LeaveRequest approveLeaveRequest(UUID id, UUID approverId) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found: " + id));

        if (request.getStatus() != LeaveStatusEnum.PENDING) {
            throw new IllegalStateException("Leave request is not pending");
        }

        request.approve(approverId.toString());
        request = leaveRequestRepository.save(request);

        log.info("Approved leave request: {} by user: {}", id, approverId);
        return request;
    }

    @Transactional
    public LeaveRequest rejectLeaveRequest(UUID id, UUID rejectorId, String reason) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found: " + id));

        if (request.getStatus() != LeaveStatusEnum.PENDING) {
            throw new IllegalStateException("Leave request is not pending");
        }

        request.reject(rejectorId.toString(), reason);
        request = leaveRequestRepository.save(request);

        log.info("Rejected leave request: {} by user: {}", id, rejectorId);
        return request;
    }

    @Transactional(readOnly = true)
    public List<LeaveBalanceEntity> getLeaveBalances(String employeeId, Integer year) {
        return leaveBalanceRepository.findByEmployeeIdAndYear(UUID.fromString(employeeId), year);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getPendingLeaveRequests() {
        return leaveRequestRepository.findByStatus(LeaveStatusEnum.PENDING);
    }
}
