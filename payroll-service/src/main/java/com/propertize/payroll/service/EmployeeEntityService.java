package com.propertize.payroll.service;

import com.propertize.payroll.dto.employee.CreateEmployeeRequest;
import com.propertize.payroll.dto.employee.EmployeeDTO;
import com.propertize.payroll.dto.employee.UpdateEmployeeRequest;
import com.propertize.payroll.entity.Client;
import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.payroll.entity.embedded.Address;
import com.propertize.payroll.entity.embedded.ContactInfo;
import com.propertize.payroll.enums.*;
import com.propertize.payroll.repository.ClientRepository;
import com.propertize.payroll.repository.EmployeeEntityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class EmployeeEntityService {

    private final EmployeeEntityRepository employeeRepository;
    private final ClientRepository clientRepository;

    public EmployeeDTO createEmployee(CreateEmployeeRequest request) {
        log.info("Creating employee with number: {}", request.getEmployeeNumber());

        if (employeeRepository.existsByEmployeeNumber(request.getEmployeeNumber())) {
            throw new IllegalArgumentException("Employee number already exists: " + request.getEmployeeNumber());
        }

        Client client = clientRepository.findById(request.getClientId())
            .orElseThrow(() -> new EntityNotFoundException("Client not found: " + request.getClientId()));

        EmployeeEntity employee = new EmployeeEntity();
        employee.setClient(client);
        employee.setExternalEmployeeId(request.getExternalEmployeeId());
        employee.setEmployeeNumber(request.getEmployeeNumber());
        employee.setFirstName(request.getFirstName());
        employee.setMiddleName(request.getMiddleName());
        employee.setLastName(request.getLastName());
        employee.setPreferredName(request.getPreferredName());
        employee.setSsnLastFour(request.getSsnLastFour());
        employee.setSsnEncrypted(request.getSsnEncrypted());
        employee.setDateOfBirth(request.getDateOfBirth());

        // Set contact info
        ContactInfo contactInfo = new ContactInfo();
        contactInfo.setEmail(request.getEmail());
        contactInfo.setPhone(request.getPhone());
        contactInfo.setMobile(request.getMobile());
        employee.setContactInfo(contactInfo);

        // Set address
        Address address = new Address();
        address.setStreet(request.getStreet());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setZipCode(request.getZipCode());
        address.setCountry(request.getCountry() != null ? request.getCountry() : "USA");
        employee.setHomeAddress(address);

        // Set employment details
        employee.setStatus(EmployeeStatusEnum.ACTIVE);
        employee.setEmploymentType(EmploymentTypeEnum.valueOf(request.getEmploymentType()));
        employee.setPayType(PayTypeEnum.valueOf(request.getPayType()));
        employee.setPayFrequency(PayFrequencyEnum.valueOf(request.getPayFrequency()));
        employee.setHourlyRate(request.getHourlyRate());
        employee.setAnnualSalary(request.getAnnualSalary());
        employee.setHireDate(request.getHireDate());
        employee.setJobTitle(request.getJobTitle());
        employee.setDepartmentId(request.getDepartmentId());
        employee.setDepartmentName(request.getDepartmentName());
        employee.setManagerId(request.getManagerId());
        employee.setWorkLocation(request.getWorkLocation());
        employee.setCostCenter(request.getCostCenter());

        EmployeeEntity saved = employeeRepository.save(employee);
        log.info("Employee created with ID: {}", saved.getId());

        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public EmployeeDTO getEmployeeById(UUID id) {
        EmployeeEntity employee = employeeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));
        return toDTO(employee);
    }

    @Transactional(readOnly = true)
    public EmployeeDTO getEmployeeByNumber(String employeeNumber) {
        EmployeeEntity employee = employeeRepository.findByEmployeeNumber(employeeNumber)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found with number: " + employeeNumber));
        return toDTO(employee);
    }

    @Transactional(readOnly = true)
    public Page<EmployeeDTO> getEmployeesByClient(UUID clientId, Pageable pageable) {
        return employeeRepository.findByClientId(clientId, pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDTO> getActiveEmployeesByClient(UUID clientId) {
        return employeeRepository.findByClientIdAndStatus(clientId, EmployeeStatusEnum.ACTIVE)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<EmployeeDTO> searchEmployees(UUID clientId, String search, Pageable pageable) {
        return employeeRepository.searchByClientId(clientId, search, pageable).map(this::toDTO);
    }

    public EmployeeDTO updateEmployee(UUID id, UpdateEmployeeRequest request) {
        log.info("Updating employee: {}", id);

        EmployeeEntity employee = employeeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));

        if (request.getFirstName() != null) employee.setFirstName(request.getFirstName());
        if (request.getMiddleName() != null) employee.setMiddleName(request.getMiddleName());
        if (request.getLastName() != null) employee.setLastName(request.getLastName());
        if (request.getPreferredName() != null) employee.setPreferredName(request.getPreferredName());

        // Update contact info
        if (request.getEmail() != null || request.getPhone() != null || request.getMobile() != null) {
            ContactInfo contactInfo = employee.getContactInfo();
            if (contactInfo == null) contactInfo = new ContactInfo();
            if (request.getEmail() != null) contactInfo.setEmail(request.getEmail());
            if (request.getPhone() != null) contactInfo.setPhone(request.getPhone());
            if (request.getMobile() != null) contactInfo.setMobile(request.getMobile());
            employee.setContactInfo(contactInfo);
        }

        // Update address
        if (request.getStreet() != null || request.getCity() != null) {
            Address address = employee.getHomeAddress();
            if (address == null) address = new Address();
            if (request.getStreet() != null) address.setStreet(request.getStreet());
            if (request.getCity() != null) address.setCity(request.getCity());
            if (request.getState() != null) address.setState(request.getState());
            if (request.getZipCode() != null) address.setZipCode(request.getZipCode());
            if (request.getCountry() != null) address.setCountry(request.getCountry());
            employee.setHomeAddress(address);
        }

        // Update employment details
        if (request.getStatus() != null) employee.setStatus(EmployeeStatusEnum.valueOf(request.getStatus()));
        if (request.getEmploymentType() != null) employee.setEmploymentType(EmploymentTypeEnum.valueOf(request.getEmploymentType()));
        if (request.getPayType() != null) employee.setPayType(PayTypeEnum.valueOf(request.getPayType()));
        if (request.getPayFrequency() != null) employee.setPayFrequency(PayFrequencyEnum.valueOf(request.getPayFrequency()));
        if (request.getHourlyRate() != null) employee.setHourlyRate(request.getHourlyRate());
        if (request.getAnnualSalary() != null) employee.setAnnualSalary(request.getAnnualSalary());
        if (request.getTerminationDate() != null) employee.setTerminationDate(request.getTerminationDate());
        if (request.getJobTitle() != null) employee.setJobTitle(request.getJobTitle());
        if (request.getDepartmentId() != null) employee.setDepartmentId(request.getDepartmentId());
        if (request.getDepartmentName() != null) employee.setDepartmentName(request.getDepartmentName());
        if (request.getManagerId() != null) employee.setManagerId(request.getManagerId());
        if (request.getManagerName() != null) employee.setManagerName(request.getManagerName());
        if (request.getWorkLocation() != null) employee.setWorkLocation(request.getWorkLocation());
        if (request.getCostCenter() != null) employee.setCostCenter(request.getCostCenter());
        if (request.getStandardHoursPerWeek() != null) employee.setStandardHoursPerWeek(request.getStandardHoursPerWeek());
        if (request.getOvertimeMultiplier() != null) employee.setOvertimeMultiplier(request.getOvertimeMultiplier());
        if (request.getOvertimeEligible() != null) employee.setOvertimeEligible(request.getOvertimeEligible());

        EmployeeEntity saved = employeeRepository.save(employee);
        log.info("Employee updated: {}", saved.getId());

        return toDTO(saved);
    }

    public void terminateEmployee(UUID id, java.time.LocalDate terminationDate) {
        log.info("Terminating employee: {} effective {}", id, terminationDate);

        EmployeeEntity employee = employeeRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Employee not found: " + id));

        employee.setStatus(EmployeeStatusEnum.TERMINATED);
        employee.setTerminationDate(terminationDate);
        employeeRepository.save(employee);
    }

    private EmployeeDTO toDTO(EmployeeEntity entity) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setId(entity.getId());
        dto.setClientId(entity.getClient().getId());
        dto.setExternalEmployeeId(entity.getExternalEmployeeId());
        dto.setEmployeeNumber(entity.getEmployeeNumber());
        dto.setFirstName(entity.getFirstName());
        dto.setMiddleName(entity.getMiddleName());
        dto.setLastName(entity.getLastName());
        dto.setPreferredName(entity.getPreferredName());
        dto.setSsnLastFour(entity.getSsnLastFour());
        dto.setDateOfBirth(entity.getDateOfBirth());

        if (entity.getContactInfo() != null) {
            dto.setEmail(entity.getContactInfo().getEmail());
            dto.setPhone(entity.getContactInfo().getPhone());
            dto.setMobile(entity.getContactInfo().getMobile());
        }

        if (entity.getHomeAddress() != null) {
            dto.setStreet(entity.getHomeAddress().getStreet());
            dto.setCity(entity.getHomeAddress().getCity());
            dto.setState(entity.getHomeAddress().getState());
            dto.setZipCode(entity.getHomeAddress().getZipCode());
            dto.setCountry(entity.getHomeAddress().getCountry());
        }

        dto.setStatus(entity.getStatus());
        dto.setEmploymentType(entity.getEmploymentType());
        dto.setPayType(entity.getPayType());
        dto.setPayFrequency(entity.getPayFrequency());
        dto.setHourlyRate(entity.getHourlyRate());
        dto.setAnnualSalary(entity.getAnnualSalary());
        dto.setHireDate(entity.getHireDate());
        dto.setTerminationDate(entity.getTerminationDate());
        dto.setJobTitle(entity.getJobTitle());
        dto.setDepartmentId(entity.getDepartmentId());
        dto.setDepartmentName(entity.getDepartmentName());
        dto.setManagerId(entity.getManagerId());
        dto.setManagerName(entity.getManagerName());
        dto.setWorkLocation(entity.getWorkLocation());
        dto.setCostCenter(entity.getCostCenter());
        dto.setStandardHoursPerWeek(entity.getStandardHoursPerWeek());
        dto.setOvertimeMultiplier(entity.getOvertimeMultiplier());
        dto.setOvertimeEligible(entity.getOvertimeEligible());
        dto.setFullName(entity.getFullName());
        dto.setDisplayName(entity.getDisplayName());
        dto.setEffectivePayRate(entity.getEffectivePayRate());

        return dto;
    }
}
