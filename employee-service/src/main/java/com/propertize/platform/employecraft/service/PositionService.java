package com.propertize.platform.employecraft.service;

import com.propertize.platform.employecraft.context.OrganizationContext;
import com.propertize.platform.employecraft.dto.PositionSummary;
import com.propertize.platform.employecraft.entity.Position;
import com.propertize.platform.employecraft.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Provides position listing for employee management workflows.
 * Scoped to the organization extracted from the gateway context.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;

    /**
     * Returns all active positions for the currently authenticated organization.
     * Used to populate position dropdowns in employee creation and edit forms.
     *
     * @return list of active position summaries, ordered by title
     */
    @Transactional(readOnly = true)
    public List<PositionSummary> getActivePositions() {
        UUID organizationId = OrganizationContext.requireOrganizationId();
        List<Position> positions = positionRepository
                .findByOrganizationIdAndIsActiveTrue(organizationId.toString());
        log.debug("Found {} active positions for org {}", positions.size(), organizationId);
        return positions.stream()
                .map(p -> PositionSummary.builder()
                        .id(p.getId())
                        .title(p.getTitle())
                        .code(p.getCode())
                        .build())
                .sorted(java.util.Comparator.comparing(PositionSummary::title, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}

