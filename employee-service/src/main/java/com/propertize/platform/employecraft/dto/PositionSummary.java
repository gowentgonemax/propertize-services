package com.propertize.platform.employecraft.dto;

import lombok.Builder;
import java.util.UUID;

@Builder
public record PositionSummary(
        UUID id,
        String title,
        String code) {
}
