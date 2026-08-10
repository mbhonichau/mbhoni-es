package com.mbhoni_creative.adminservice.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.adminentity.FieldDefinition;
import com.mbhoni_creative.adminentity.FieldSection;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminrepository.FieldDefinitionRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.FieldDefinitionService;
import com.mbhoni_creative.config.TenantSecurityService;

@Service
@Transactional
public class FieldDefinitionServiceImpl implements FieldDefinitionService {

    private final FieldDefinitionRepository fieldDefinitionRepository;
    private final TenantRepository tenantRepository;
    private final TenantSecurityService tenantSecurityService;

    public FieldDefinitionServiceImpl(
            FieldDefinitionRepository fieldDefinitionRepository,
            TenantRepository tenantRepository,
            TenantSecurityService tenantSecurityService) {
        this.fieldDefinitionRepository = fieldDefinitionRepository;
        this.tenantRepository = tenantRepository;
        this.tenantSecurityService = tenantSecurityService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldDefinition> getEffectiveSchema(Long tenantId, FieldSection section) {
        return fieldDefinitionRepository.findActiveFieldsWithFallback(tenantId, section);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldDefinition> getAllEffectiveSchemaForTenant(Long tenantId) {
        List<FieldDefinition> result = new ArrayList<>();
        for (FieldSection sec : FieldSection.values()) {
            result.addAll(getEffectiveSchema(tenantId, sec));
        }
        return result;
    }

    @Override
    public FieldDefinition createField(Long tenantId, FieldDefinition definition) {
        assertAccess(tenantId);
        if (definition.getSection() == null) {
            throw new IllegalArgumentException("FieldSection is required.");
        }
        if (definition.getFieldKey() == null || definition.getFieldKey().isBlank()) {
            throw new IllegalArgumentException("fieldKey is required.");
        }

        String normalizedKey = definition.getFieldKey().replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
        definition.setFieldKey(normalizedKey);

        // Validate fieldKey uniqueness per tenant + section before create
        Optional<FieldDefinition> existingOpt = tenantId != null
                ? fieldDefinitionRepository.findByTenantIdAndSectionAndFieldKey(tenantId, definition.getSection(), normalizedKey)
                : fieldDefinitionRepository.findByTenantIdIsNullAndSectionAndFieldKey(definition.getSection(), normalizedKey);

        if (existingOpt.isPresent()) {
            throw new IllegalArgumentException("Field key '" + normalizedKey + "' already exists for section " + definition.getSection());
        }

        if (tenantId != null) {
            Tenant tenant = tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant not found ID: " + tenantId));
            definition.setTenant(tenant);
        } else {
            definition.setTenant(null);
        }

        definition.setActive(true);
        return fieldDefinitionRepository.save(definition);
    }

    @Override
    public FieldDefinition updateField(Long tenantId, Long fieldId, FieldDefinition dto) {
        assertAccess(tenantId);
        FieldDefinition existing = fieldDefinitionRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("FieldDefinition not found ID: " + fieldId));

        if (dto.getLabel() != null && !dto.getLabel().isBlank()) {
            existing.setLabel(dto.getLabel());
        }
        if (dto.getDataType() != null) {
            existing.setDataType(dto.getDataType());
        }
        if (dto.getSelectOptions() != null) {
            existing.setSelectOptions(dto.getSelectOptions());
        }
        existing.setRequired(dto.isRequired());
        existing.setDisplayOrder(dto.getDisplayOrder());
        existing.setValidationRule(dto.getValidationRule());

        return fieldDefinitionRepository.save(existing);
    }

    @Override
    public void deactivateField(Long tenantId, Long fieldId) {
        assertAccess(tenantId);
        FieldDefinition existing = fieldDefinitionRepository.findById(fieldId)
                .orElseThrow(() -> new RuntimeException("FieldDefinition not found ID: " + fieldId));

        // Soft deactivation only — no hard delete once EmployeeFieldValue rows reference it!
        existing.setActive(false);
        fieldDefinitionRepository.save(existing);
    }

    private void assertAccess(Long tenantId) {
        if (tenantSecurityService.isGlobalAdmin()) {
            return;
        }
        Long currentTenantId = tenantSecurityService.getCurrentTenantId();
        if (currentTenantId == null || (tenantId != null && !currentTenantId.equals(tenantId))) {
            throw new RuntimeException("Access denied: Tenant ID " + tenantId);
        }
    }
}
