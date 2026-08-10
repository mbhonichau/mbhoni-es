package com.mbhoni_creative.adminservice;

import java.util.List;
import com.mbhoni_creative.admindto.OnboardingSchemaResponse;
import com.mbhoni_creative.admindto.TenantOnboardingFieldDto;
import com.mbhoni_creative.adminentity.TargetEntity;

public interface TenantOnboardingFieldService {

    List<TenantOnboardingFieldDto> getFieldsForTenant(Long tenantId, TargetEntity targetEntity);

    OnboardingSchemaResponse getOnboardingSchemaForTenant(Long tenantId);

    void saveOrUpdateFields(Long tenantId, List<TenantOnboardingFieldDto> dtos);

    TenantOnboardingFieldDto addCustomField(Long tenantId, TenantOnboardingFieldDto dto);

    void deleteField(Long tenantId, Long fieldId);

    void seedDefaultFieldsForTenant(Long tenantId);

    void applyIndustryProfileFields(Long tenantId, Long industryProfileId);
}
