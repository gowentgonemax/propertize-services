package com.propertize.payroll.repository;

import com.propertize.payroll.entity.PerformanceReviewEntity;
import com.propertize.payroll.enums.ReviewStatusEnum;
import com.propertize.payroll.enums.ReviewTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface PerformanceReviewRepository extends JpaRepository<PerformanceReviewEntity, UUID> {

    List<PerformanceReviewEntity> findByEmployeeIdOrderByReviewDateDesc(UUID employeeId);

    Page<PerformanceReviewEntity> findByEmployeeId(UUID employeeId, Pageable pageable);

    List<PerformanceReviewEntity> findByStatus(ReviewStatusEnum status);

    @Query("SELECT pr FROM PerformanceReviewEntity pr WHERE pr.employee.id = :employeeId AND pr.reviewType = :reviewType ORDER BY pr.reviewDate DESC")
    List<PerformanceReviewEntity> findByEmployeeIdAndReviewType(
            @Param("employeeId") UUID employeeId,
            @Param("reviewType") ReviewTypeEnum reviewType);

    @Query("SELECT pr FROM PerformanceReviewEntity pr WHERE pr.employee.id = :employeeId AND pr.reviewDate BETWEEN :startDate AND :endDate")
    List<PerformanceReviewEntity> findByEmployeeIdAndDateRange(
            @Param("employeeId") UUID employeeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT pr FROM PerformanceReviewEntity pr WHERE pr.reviewer = :reviewerId AND pr.status = :status")
    List<PerformanceReviewEntity> findByReviewerIdAndStatus(
            @Param("reviewerId") String reviewerId,
            @Param("status") ReviewStatusEnum status);
}
