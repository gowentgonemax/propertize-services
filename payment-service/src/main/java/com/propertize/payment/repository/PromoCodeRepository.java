package com.propertize.payment.repository;

import com.propertize.payment.entity.PromoCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, String> {

    Optional<PromoCode> findByCodeIgnoreCase(String code);

    Optional<PromoCode> findByCodeIgnoreCaseAndActiveTrue(String code);

    Optional<PromoCode> findByCodeIgnoreCaseAndOrganizationId(String code, String organizationId);

    Page<PromoCode> findByOrganizationId(String orgId, Pageable pageable);

    boolean existsByCodeIgnoreCase(String code);
}
