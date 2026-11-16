package com.mbhoni_creative.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class WorkforceDtos {
    private WorkforceDtos() {}
    public record EmployeeSummary(Long id, String employeeNumber, String firstName, String lastName, String jobTitle,
            String status, String employmentType, Long orgUnitId, Long managerId, LocalDate startDate,
            LocalDateTime updatedAt, Map<String, String> customFields) {}
    public record EmployeeDetail(Long id, String employeeNumber, String firstName, String lastName, String jobTitle,
            String status, String employmentType, Long orgUnitId, Long managerId, LocalDate startDate,
            LocalDateTime updatedAt, Map<String, String> customFields) {}
    public record Field(Long id, String key, String label, String section, String type, boolean required,
            List<String> allowedOptions, int displayOrder) {}
    public record OrgUnit(Long id, String name, String code, Long parentId, String type, LocalDateTime updatedAt) {}
    public record Reference(Long id, String code, String value, String locale) {}
}
