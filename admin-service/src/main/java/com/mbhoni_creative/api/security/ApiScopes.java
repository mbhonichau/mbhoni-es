package com.mbhoni_creative.api.security;

import com.mbhoni_creative.adminentity.Permission;
import java.util.*;
import java.util.stream.Collectors;

public final class ApiScopes {
    private ApiScopes() {}
    public static final Map<String, Set<String>> PERMISSION_SCOPES = Map.of(
            "USER_VIEW", Set.of("users:read", "access:check"),
            "TENANT_VIEW", Set.of("tenant:read", "quotas:read"),
            "CUSTOMIZATION_VIEW", Set.of("branding:read"),
            "MODULE_VIEW", Set.of("modules:read"),
            "SUBSCRIPTION_VIEW", Set.of("entitlements:read"));
    public static Set<String> from(Collection<Permission> permissions) {
        return permissions.stream().flatMap(p -> PERMISSION_SCOPES.getOrDefault(p.getName(), Set.of()).stream())
                .collect(Collectors.toUnmodifiableSet());
    }
}
