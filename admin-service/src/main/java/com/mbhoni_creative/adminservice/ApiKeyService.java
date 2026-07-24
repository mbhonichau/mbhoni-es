package com.mbhoni_creative.adminservice;

import java.util.List;

import com.mbhoni_creative.admindto.ApiKeyDto;
import com.mbhoni_creative.admindto.GeneratedApiKey;
import com.mbhoni_creative.adminentity.Permission;
import com.mbhoni_creative.adminentity.TenantApiKey;

public interface ApiKeyService {

    List<ApiKeyDto> getApiKeys();

    ApiKeyDto prepareCreate();

    List<Permission> getAllPermissions();

    GeneratedApiKey createApiKey(ApiKeyDto dto);

    void revokeApiKey(Long id);

    TenantApiKey authenticate(String rawKey);
}
