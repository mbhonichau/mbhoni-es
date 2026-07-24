package com.mbhoni_creative.adminservice;

import java.util.List;

import com.mbhoni_creative.admindto.TenantBrandingResponse;
import com.mbhoni_creative.adminentity.TenantCustomization;

public interface TenantCustomizationService {

    List<TenantCustomization> getAllCustomizations();

    TenantCustomization getOrCreateForTenant(Long tenantId);

    TenantCustomization getOrCreateForCurrentTenant();

    TenantCustomization saveForTenant(Long tenantId, TenantCustomization source);

    TenantCustomization saveForCurrentTenant(TenantCustomization source);

    boolean brandingAllowed(Long tenantId);
    
    TenantBrandingResponse getBrandingResponse(Long tenantId);
    
    TenantBrandingResponse getBrandingResponseByDomain(String domain);
}