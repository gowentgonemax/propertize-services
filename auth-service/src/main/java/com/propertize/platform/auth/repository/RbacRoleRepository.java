package com.propertize.platform.auth.repository;

import com.propertize.platform.auth.entity.RbacRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RbacRoleRepository extends JpaRepository<RbacRole, Long> {

    Optional<RbacRole> findByRoleNameAndIsActiveTrue(String roleName);

    List<RbacRole> findByIsSystemTrueAndIsActiveTrue();

    List<RbacRole> findByIsSystemFalseAndIsActiveTrue();

    List<RbacRole> findByOrganizationIdAndIsActiveTrue(Long organizationId);

    Optional<RbacRole> findByRoleNameAndOrganizationIdAndIsActiveTrue(String roleName, Long organizationId);

    boolean existsByRoleNameAndIsSystemTrue(String roleName);

    boolean existsByRoleNameAndOrganizationIdAndIsActiveTrue(String roleName, Long organizationId);
}
