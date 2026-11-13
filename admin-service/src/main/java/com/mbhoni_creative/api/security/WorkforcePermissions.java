package com.mbhoni_creative.api.security;

import com.mbhoni_creative.adminentity.Permission;
import com.mbhoni_creative.adminentity.TenantApiKey;
import java.util.Set;

public final class WorkforcePermissions {
    private WorkforcePermissions() {}
    public static final Set<String> API_ONLY = Set.of("API_EMPLOYEE_READ", "API_ORG_READ", "API_REFERENCE_READ");
    public static boolean v1Only(TenantApiKey key) {
        return key.getPermissions() != null && !key.getPermissions().isEmpty()
                && key.getPermissions().stream().map(Permission::getName).allMatch(API_ONLY::contains);
    }
}
