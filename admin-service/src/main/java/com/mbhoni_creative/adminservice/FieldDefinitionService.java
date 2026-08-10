package com.mbhoni_creative.adminservice;

import java.util.List;
import com.mbhoni_creative.adminentity.FieldDefinition;
import com.mbhoni_creative.adminentity.FieldSection;

public interface FieldDefinitionService {

    List<FieldDefinition> getEffectiveSchema(Long tenantId, FieldSection section);

    List<FieldDefinition> getAllEffectiveSchemaForTenant(Long tenantId);

    FieldDefinition createField(Long tenantId, FieldDefinition definition);

    FieldDefinition updateField(Long tenantId, Long fieldId, FieldDefinition definition);

    void deactivateField(Long tenantId, Long fieldId);
}
