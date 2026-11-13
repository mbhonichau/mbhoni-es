package com.mbhoni_creative.api.mapper;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.api.dto.ApiDtos.*;
import com.mbhoni_creative.config.CustomUserPrincipal;
import java.util.*;
import java.util.stream.Collectors;

public final class ApiMapper {
    private ApiMapper() {}
    public static Set<String> permissions(User user) {
        return new CustomUserPrincipal(user).getAuthorities().stream().map(a -> a.getAuthority())
                .filter(a -> !a.startsWith("ROLE_")).collect(Collectors.toCollection(TreeSet::new));
    }
    public static UserProfile user(User user) {
        Set<String> roles = user.getRoles().stream().map(Role::getName).filter(Objects::nonNull)
                .collect(Collectors.toCollection(TreeSet::new));
        return new UserProfile(user.getId(), user.getUsername(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.isActive(), roles, permissions(user));
    }
    public static Settings settings(TenantSettings settings) {
        // Entity defaults are in-memory only; never saved by this adapter.
        TenantSettings s = settings == null ? new TenantSettings() : settings;
        return new Settings(s.isApiAccessEnabled(), s.getRateLimitPerMinute(), s.isMaintenanceMode(),
                s.isSelfRegistrationAllowed(), s.getMaxFileUploadMb());
    }
}
