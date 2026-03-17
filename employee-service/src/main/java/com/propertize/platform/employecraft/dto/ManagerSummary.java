package com.propertize.platform.employecraft.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public  class ManagerSummary {
    private UUID id;
    private String fullName;
    private String email;
}
