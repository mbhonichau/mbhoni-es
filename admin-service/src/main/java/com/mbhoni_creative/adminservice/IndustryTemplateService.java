package com.mbhoni_creative.adminservice;

public interface IndustryTemplateService {

    void applyTemplateToTenant(Long tenantId);

    void applyModuleTemplateToTenant(Long tenantId);

    void applyRoleTemplateToTenant(Long tenantId);
    
    
}