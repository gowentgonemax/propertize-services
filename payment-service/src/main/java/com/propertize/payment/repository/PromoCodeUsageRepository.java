package com.propertize.payment.repository;

import com.propertize.payment.entity.PromoCodeUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, String> {

    List<PromoCodeUsage> findByPromoCodeId(String promoCodeId);

    Page<PromoCodeUsage> findByPromoCodeId(String promoCodeId, Pageable pageable);

    boolean existsByPromoCodeIdAndApplicationId(String promoCodeId, String applicationId);
}
