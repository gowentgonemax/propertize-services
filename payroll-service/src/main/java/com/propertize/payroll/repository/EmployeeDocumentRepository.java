package com.propertize.payroll.repository;

import com.propertize.payroll.entity.EmployeeDocumentEntity;
import com.propertize.payroll.enums.DocumentTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocumentEntity, UUID> {

    List<EmployeeDocumentEntity> findByEmployeeId(String employeeId);

    Page<EmployeeDocumentEntity> findByEmployeeId(String employeeId, Pageable pageable);

    List<EmployeeDocumentEntity> findByEmployeeIdAndDocumentType(String employeeId, DocumentTypeEnum documentType);

    List<EmployeeDocumentEntity> findByDocumentType(DocumentTypeEnum documentType);

    List<EmployeeDocumentEntity> findByExpiryDateBefore(LocalDate date);

    List<EmployeeDocumentEntity> findByEmployeeIdAndIsVerified(String employeeId, Boolean isVerified);

    long countByEmployeeId(String employeeId);
}
