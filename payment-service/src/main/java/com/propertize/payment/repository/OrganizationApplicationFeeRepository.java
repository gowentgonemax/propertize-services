package com.propertize.payment.repository;

import com.propertize.payment.entity.OrganizationApplicationFee;
import com.propertize.payment.enums.PaymentStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationApplicationFeeRepository extends JpaRepository<OrganizationApplicationFee, String> {

    Optional<OrganizationApplicationFee> findByTrackingId(String trackingId);

    Optional<OrganizationApplicationFee> findByOrganizationApplicationId(String applicationId);

    Optional<OrganizationApplicationFee> findByStripePaymentIntentId(String intentId);

    Page<OrganizationApplicationFee> findByOrganizationId(String orgId, Pageable pageable);

    Page<OrganizationApplicationFee> findByOrganizationIdAndPaymentStatus(String orgId, PaymentStatusEnum status,
            Pageable pageable);

    @Query("SELECT f FROM OrganizationApplicationFee f WHERE LOWER(f.applicantEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(f.organizationName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<OrganizationApplicationFee> search(@Param("search") String search, Pageable pageable);
}
