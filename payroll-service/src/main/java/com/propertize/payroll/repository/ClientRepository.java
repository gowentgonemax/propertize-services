package com.propertize.payroll.repository;

import com.propertize.payroll.entity.Client;
import com.propertize.payroll.enums.ClientStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    Page<Client> findByStatus(ClientStatusEnum status, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Client c WHERE c.taxId = :taxId")
    boolean existsByTaxId(@Param("taxId") String taxId);

    @Override
    @EntityGraph(attributePaths = {})
    Optional<Client> findById(UUID id);
}
