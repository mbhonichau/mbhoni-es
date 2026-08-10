package com.mbhoni_creative.adminservice;

import com.mbhoni_creative.admindto.TenantSettingsDto;
import com.mbhoni_creative.adminentity.TenantSettings;

public interface TenantSettingsService {

    TenantSettings getOrCreateEntityForTenant(Long tenantId);

    TenantSettingsDto getSettingsForTenant(Long tenantId);

    TenantSettingsDto getSettingsForCurrentTenant();

    TenantSettingsDto updateSettingsForTenant(Long tenantId, TenantSettingsDto dto);

    TenantSettingsDto updateSettingsForCurrentTenant(TenantSettingsDto dto);
}
