package com.propertize.payment.repository;

import com.propertize.payment.entity.ApplicationFee;
import com.propertize.commons.enums.payment.PaymentStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationFeeRepository extends JpaRepository<ApplicationFee, String> {

    Optional<ApplicationFee> findByRentalApplicationId(String rentalApplicationId);

    Page<ApplicationFee> findByOrganizationId(String orgId, Pageable pageable);

    Page<ApplicationFee> findByOrganizationIdAndPaymentStatus(String orgId, PaymentStatusEnum status,
            Pageable pageable);

    @Query("SELECT f FROM ApplicationFee f WHERE f.paymentStatus = 'PENDING' AND f.dueDate < :now")
    List<ApplicationFee> findOverdueFees(@Param("now") OffsetDateTime now);
}
