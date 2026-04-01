package com.propertize.platform.auth.service;

import com.propertize.platform.auth.config.RbacConfig;
import com.propertize.platform.auth.entity.RbacRole;
import com.propertize.platform.auth.repository.RbacRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Seeds system roles from {@code rbac.yml} into the {@code rbac_roles} table at
 * startup.
 *
 * <p>
 * This runner executes after the Spring context (and Flyway) are fully
 * initialised.
 * It performs an <em>upsert</em>: roles present in YAML are created if they
 * don't
 * exist, or updated if they do. This means YAML remains the authoritative
 * source
 * for system-role definitions; the DB is a materialised view of it that also
 * hosts runtime custom roles.
 * </p>
 *
 * <p>
 * Custom roles ({@code isSystem=false}) are never touched by this seeder.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RbacSeederService implements ApplicationRunner {

    private final RbacConfig rbacConfig;
    private final RbacRoleRepository rbacRoleRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (rbacConfig.getRoles() == null || rbacConfig.getRoles().isEmpty()) {
            log.warn("⚠️ No RBAC roles found in config — skipping DB seed");
            return;
        }

        int created = 0;
        int updated = 0;

        for (Map.Entry<String, RbacConfig.RoleConfig> entry : rbacConfig.getRoles().entrySet()) {
            String roleName = entry.getKey(); // e.g. "PROPERTY_MANAGER"
            RbacConfig.RoleConfig cfg = entry.getValue();

            String permsCsv = buildPermissionCsv(cfg);
            String inheritsFrom = buildInheritsFrom(cfg);
            String displayName = toTitleCase(roleName.replace('_', ' ').toLowerCase());

            var existing = rbacRoleRepository.findByRoleNameAndIsActiveTrue(roleName);
            if (existing.isPresent()) {
                RbacRole role = existing.get();
                role.setDisplayName(displayName);
                role.setDescription(cfg.getDescription());
                role.setScope(cfg.getScope() != null ? cfg.getScope() : "self");
                role.setLevel(cfg.getLevel() != null ? cfg.getLevel() : 0);
                role.setCategory(cfg.getCategory() != null ? cfg.getCategory() : "");
                role.setPermissions(permsCsv);
                role.setInheritsFrom(inheritsFrom);
                role.setApplicableOrgTypes(buildCsv(cfg.getApplicableOrgTypes()));
                role.setExplicitDenials(buildCsv(cfg.getExplicitDenials()));
                role.setUpdatedAt(LocalDateTime.now());
                rbacRoleRepository.save(role);
                updated++;
            } else {
                RbacRole role = RbacRole.builder()
                        .roleName(roleName)
                        .displayName(displayName)
                        .description(cfg.getDescription())
                        .scope(cfg.getScope() != null ? cfg.getScope() : "self")
                        .level(cfg.getLevel() != null ? cfg.getLevel() : 0)
                        .category(cfg.getCategory() != null ? cfg.getCategory() : "")
                        .permissions(permsCsv)
                        .inheritsFrom(inheritsFrom)
                        .applicableOrgTypes(buildCsv(cfg.getApplicableOrgTypes()))
                        .explicitDenials(buildCsv(cfg.getExplicitDenials()))
                        .isSystem(true)
                        .organizationId(null)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                rbacRoleRepository.save(role);
                created++;
            }
        }

        log.info("✅ RBAC seeder complete — {} system roles created, {} updated", created, updated);
    }

    private String buildPermissionCsv(RbacConfig.RoleConfig cfg) {
        return buildCsv(cfg.getPermissions());
    }

    private String buildCsv(java.util.List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return String.join(",", items);
    }

    private String buildInheritsFrom(RbacConfig.RoleConfig cfg) {
        if (cfg.getInherits() == null || cfg.getInherits().isEmpty()) {
            return null;
        }
        return String.join(",", cfg.getInherits());
    }

    private String toTitleCase(String spacedName) {
        if (spacedName == null || spacedName.isBlank())
            return spacedName;
        String[] words = spacedName.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty())
                continue;
            if (sb.length() > 0)
                sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }
}
