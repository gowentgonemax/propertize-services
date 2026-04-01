package com.propertize.platform.employecraft.dto;

import lombok.Builder;
import java.util.UUID;

@Builder
public record ManagerSummary(
        UUID id,
        String fullName,
        String email) {
}
