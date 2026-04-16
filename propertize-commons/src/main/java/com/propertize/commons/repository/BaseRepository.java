package com.propertize.commons.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

/**
 * Shared base repository with common query methods.
 * All entity repositories should extend this interface.
 *
 * @param <T>  Entity type
 * @param <ID> ID type
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends
        JpaRepository<T, ID>,
        JpaSpecificationExecutor<T> {

    /**
     * Find all non-deleted (soft delete) entities.
     * Works for entities extending AuditableEntity.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.deletedAt IS NULL")
    List<T> findAllActive();
}

