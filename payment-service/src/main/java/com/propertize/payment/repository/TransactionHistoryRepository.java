package com.propertize.payment.repository;

import com.propertize.payment.entity.TransactionHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, String> {

        Optional<TransactionHistory> findByReferenceNumber(String referenceNumber);

        Optional<TransactionHistory> findByProviderReferenceId(String providerReferenceId);

        Page<TransactionHistory> findByOrganizationIdOrderByTransactionDateDesc(String orgId, Pageable pageable);

        Page<TransactionHistory> findByTenantIdOrderByTransactionDateDesc(String tenantId, Pageable pageable);

        Page<TransactionHistory> findByPropertyIdOrderByTransactionDateDesc(String propertyId, Pageable pageable);

        List<TransactionHistory> findByLeaseId(String leaseId);

        List<TransactionHistory> findByInvoiceId(String invoiceId);

        @Query("SELECT t FROM TransactionHistory t WHERE t.organizationId = :orgId AND t.transactionDate BETWEEN :start AND :end")
        List<TransactionHistory> findByOrganizationAndDateRange(
                        @Param("orgId") String orgId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT t FROM TransactionHistory t WHERE t.tenantId = :tenantId AND t.transactionDate BETWEEN :start AND :end")
        List<TransactionHistory> findByTenantAndDateRange(
                        @Param("tenantId") String tenantId,
                        @Param("start") LocalDateTime start,
                        @Param("end") LocalDateTime end);

        @Query("SELECT t FROM TransactionHistory t WHERE t.organizationId = :orgId AND " +
                        "(LOWER(t.referenceNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        " LOWER(t.description) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<TransactionHistory> searchTransactions(
                        @Param("orgId") String orgId,
                        @Param("search") String search,
                        Pageable pageable);

        @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(t.referenceNumber, 13) AS int)), 0) FROM TransactionHistory t WHERE t.referenceNumber LIKE :prefix%")
        Integer findMaxSequenceForPrefix(@Param("prefix") String prefix);
}
