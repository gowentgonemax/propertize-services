package com.propertize.payroll.dto.department.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponse {
    private UUID id;
    private String name;
    private String departmentCode;
    private String description;
    private ParentDepartmentSummary parentDepartment;
    private String costCenter;
    private String glAccountCode;
    private ManagerSummary manager;
    private String defaultWorkLocation;
    private String stateCode;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentDepartmentSummary {
        private UUID id;
        private String name;
        private String departmentCode;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManagerSummary {
        private UUID id;
        private String firstName;
        private String lastName;
        private String email;
    }
}
