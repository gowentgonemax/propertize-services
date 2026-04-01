package com.propertize.platform.employecraft.dto;

import lombok.Builder;
import java.util.UUID;

@Builder
public record DepartmentSummary(
        UUID id,
        String name,
        String code) {
}
