package com.propertize.payroll.repository;

import com.propertize.payroll.entity.DirectDepositAccountEntity;
import com.propertize.payroll.enums.BankAccountStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeBankAccountRepository extends JpaRepository<DirectDepositAccountEntity, UUID> {

    List<DirectDepositAccountEntity> findByEmployeeId(UUID employeeId);

    Optional<DirectDepositAccountEntity> findByEmployeeIdAndIsPrimaryTrue(UUID employeeId);

    List<DirectDepositAccountEntity> findByEmployeeIdAndStatus(UUID employeeId, BankAccountStatusEnum status);

    @Query("SELECT dda FROM DirectDepositAccountEntity dda WHERE dda.employee.id = :employeeId ORDER BY dda.isPrimary DESC, dda.createdAt ASC")
    List<DirectDepositAccountEntity> findAllByEmployeeIdOrdered(@Param("employeeId") UUID employeeId);

    @Query("SELECT dda FROM DirectDepositAccountEntity dda WHERE dda.status = :status")
    List<DirectDepositAccountEntity> findByStatus(@Param("status") BankAccountStatusEnum status);

    @Query("SELECT COUNT(dda) FROM DirectDepositAccountEntity dda WHERE dda.employee.id = :employeeId AND dda.status = 'ACTIVE'")
    long countActiveAccountsByEmployeeId(@Param("employeeId") UUID employeeId);
}
