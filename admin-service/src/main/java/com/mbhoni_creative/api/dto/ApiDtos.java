package com.mbhoni_creative.api.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.Set;

/** Explicit allowlists. No entities, credentials, or internal audit fields cross the API boundary. */
public final class ApiDtos {
    private ApiDtos() {}
    public record Identity(String type, Long id, String name, Long tenantId, Set<String> roles, Set<String> permissions, Set<String> scopes) {}
    public record UserProfile(Long id, String username, String firstName, String lastName, String email, boolean active,
                              Set<String> roles, Set<String> permissions) {}
    public record AccessCheck(@NotNull @Positive Long userId, @NotBlank @Size(max = 100) String permission) {}
    public record AccessResult(boolean allowed) {}
    public record Settings(boolean apiAccessEnabled, Integer rateLimitPerMinute, boolean maintenanceMode,
                           boolean selfRegistrationAllowed, Integer maxFileUploadMb) {}
    public record TenantProfile(Long id, String code, String name, String status, boolean active, Settings settings) {}
    public record Branding(String displayName, String logoUrl, String primaryColor, String secondaryColor) {}
    public record Quotas(Integer maxUsers, long currentUsers, Integer maxStorageGb, Long currentStorageBytes,
                         Integer maxApiRequestsPerMinute, Long currentApiRequestsPerMinute) {}
    public record Module(String code, String name, boolean enabled, LocalDateTime updatedAt) {}
    public record Entitlement(String module, boolean enabled, String plan, Instant validUntil) {}
}
