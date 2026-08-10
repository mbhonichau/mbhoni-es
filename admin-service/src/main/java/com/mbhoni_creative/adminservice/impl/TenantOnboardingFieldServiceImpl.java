package com.mbhoni_creative.adminservice.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.admindto.OnboardingSchemaResponse;
import com.mbhoni_creative.admindto.TenantOnboardingFieldDto;
import com.mbhoni_creative.adminentity.IndustryProfileField;
import com.mbhoni_creative.adminentity.RequirementState;
import com.mbhoni_creative.adminentity.TargetEntity;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantOnboardingField;
import com.mbhoni_creative.adminrepository.IndustryProfileFieldRepository;
import com.mbhoni_creative.adminrepository.TenantOnboardingFieldRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.TenantOnboardingFieldService;
import com.mbhoni_creative.config.TenantSecurityService;

@Service
public class TenantOnboardingFieldServiceImpl implements TenantOnboardingFieldService {

    private final TenantOnboardingFieldRepository onboardingFieldRepository;
    private final IndustryProfileFieldRepository industryFieldRepository;
    private final TenantRepository tenantRepository;
    private final TenantSecurityService tenantSecurityService;

    public TenantOnboardingFieldServiceImpl(
            TenantOnboardingFieldRepository onboardingFieldRepository,
            IndustryProfileFieldRepository industryFieldRepository,
            TenantRepository tenantRepository,
            TenantSecurityService tenantSecurityService) {

        this.onboardingFieldRepository = onboardingFieldRepository;
        this.industryFieldRepository = industryFieldRepository;
        this.tenantRepository = tenantRepository;
        this.tenantSecurityService = tenantSecurityService;
    }

    @Override
    @Transactional
    public List<TenantOnboardingFieldDto> getFieldsForTenant(Long tenantId, TargetEntity targetEntity) {
        assertAccess(tenantId);
        seedDefaultFieldsForTenant(tenantId);

        List<TenantOnboardingField> fields = targetEntity != null
                ? onboardingFieldRepository.findByTenantIdAndTargetEntityOrderByDisplayOrderAsc(tenantId, targetEntity)
                : onboardingFieldRepository.findByTenantIdOrderByDisplayOrderAsc(tenantId);

        return fields.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OnboardingSchemaResponse getOnboardingSchemaForTenant(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found ID: " + tenantId));

        seedDefaultFieldsForTenant(tenantId);

        OnboardingSchemaResponse response = new OnboardingSchemaResponse();
        response.setTenantId(tenant.getId());
        response.setTenantName(tenant.getName());
        if (tenant.getIndustryProfile() != null) {
            response.setIndustryProfileName(tenant.getIndustryProfile().getName());
        }

        response.setUserFields(
                onboardingFieldRepository.findByTenantIdAndTargetEntityOrderByDisplayOrderAsc(tenantId, TargetEntity.USER)
                        .stream().map(this::mapToDto).collect(Collectors.toList())
        );

        response.setEmployeeFields(
                onboardingFieldRepository.findByTenantIdAndTargetEntityOrderByDisplayOrderAsc(tenantId, TargetEntity.EMPLOYEE)
                        .stream().map(this::mapToDto).collect(Collectors.toList())
        );

        return response;
    }

    @Override
    @Transactional
    public void saveOrUpdateFields(Long tenantId, List<TenantOnboardingFieldDto> dtos) {
        assertAccess(tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found ID: " + tenantId));

        for (TenantOnboardingFieldDto dto : dtos) {
            if (dto.getFieldKey() == null || dto.getFieldKey().isBlank()) {
                continue;
            }

            TargetEntity target = dto.getTargetEntity() != null ? dto.getTargetEntity() : TargetEntity.EMPLOYEE;

            TenantOnboardingField entity = onboardingFieldRepository
                    .findByTenantIdAndTargetEntityAndFieldKey(tenantId, target, dto.getFieldKey())
                    .orElseGet(() -> {
                        TenantOnboardingField newEntity = new TenantOnboardingField();
                        newEntity.setTenant(tenant);
                        newEntity.setTargetEntity(target);
                        newEntity.setFieldKey(dto.getFieldKey());
                        return newEntity;
                    });

            if (dto.getFieldLabel() != null && !dto.getFieldLabel().isBlank()) {
                entity.setFieldLabel(dto.getFieldLabel());
            }
            if (dto.getFieldCategory() != null) {
                entity.setFieldCategory(dto.getFieldCategory());
            }
            if (dto.getRequirementState() != null) {
                entity.setRequirementState(dto.getRequirementState());
            }
            if (dto.getDisplayOrder() != null) {
                entity.setDisplayOrder(dto.getDisplayOrder());
            }
            entity.setHelpText(dto.getHelpText());

            onboardingFieldRepository.save(entity);
        }
    }

    @Override
    @Transactional
    public TenantOnboardingFieldDto addCustomField(Long tenantId, TenantOnboardingFieldDto dto) {
        assertAccess(tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found ID: " + tenantId));

        TargetEntity target = dto.getTargetEntity() != null ? dto.getTargetEntity() : TargetEntity.EMPLOYEE;
        String fieldKey = dto.getFieldKey();
        if (fieldKey == null || fieldKey.isBlank()) {
            fieldKey = "custom_" + System.currentTimeMillis();
        } else {
            fieldKey = fieldKey.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
        }

        TenantOnboardingField entity = new TenantOnboardingField();
        entity.setTenant(tenant);
        entity.setTargetEntity(target);
        entity.setFieldKey(fieldKey);
        entity.setFieldLabel(dto.getFieldLabel() != null && !dto.getFieldLabel().isBlank() ? dto.getFieldLabel() : "New Custom Field");
        entity.setFieldCategory(dto.getFieldCategory() != null ? dto.getFieldCategory() : "GENERAL");
        entity.setRequirementState(dto.getRequirementState() != null ? dto.getRequirementState() : RequirementState.OPTIONAL);
        entity.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 99);
        entity.setHelpText(dto.getHelpText());

        TenantOnboardingField saved = onboardingFieldRepository.save(entity);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public void deleteField(Long tenantId, Long fieldId) {
        assertAccess(tenantId);
        onboardingFieldRepository.findById(fieldId).ifPresent(field -> {
            if (field.getTenant() != null && field.getTenant().getId().equals(tenantId)) {
                onboardingFieldRepository.delete(field);
            }
        });
    }

    @Override
    @Transactional
    public void seedDefaultFieldsForTenant(Long tenantId) {
        List<TenantOnboardingField> existing = onboardingFieldRepository.findByTenantIdOrderByDisplayOrderAsc(tenantId);
        if (!existing.isEmpty()) {
            return;
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found ID: " + tenantId));

        List<TenantOnboardingField> catalog = createDefaultFieldCatalog(tenant);
        onboardingFieldRepository.saveAll(catalog);

        // If tenant has an assigned industry profile, apply industry presets over defaults
        if (tenant.getIndustryProfile() != null) {
            applyIndustryProfileFields(tenantId, tenant.getIndustryProfile().getId());
        }
    }

    @Override
    @Transactional
    public void applyIndustryProfileFields(Long tenantId, Long industryProfileId) {
        List<IndustryProfileField> templateFields = industryFieldRepository
                .findByIndustryProfileIdOrderByDisplayOrderAsc(industryProfileId);

        if (templateFields.isEmpty()) {
            return;
        }

        for (IndustryProfileField template : templateFields) {
            Optional<TenantOnboardingField> tenantFieldOpt = onboardingFieldRepository
                    .findByTenantIdAndTargetEntityAndFieldKey(tenantId, template.getTargetEntity(), template.getFieldKey());

            if (tenantFieldOpt.isPresent()) {
                TenantOnboardingField tenantField = tenantFieldOpt.get();
                tenantField.setRequirementState(template.getRequirementState());
                if (template.getFieldLabel() != null && !template.getFieldLabel().isBlank()) {
                    tenantField.setFieldLabel(template.getFieldLabel());
                }
                onboardingFieldRepository.save(tenantField);
            }
        }
    }

    private List<TenantOnboardingField> createDefaultFieldCatalog(Tenant tenant) {
        List<TenantOnboardingField> list = new ArrayList<>();
        int order = 10;

        // IDENTITY & PERSONAL
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "nationalId", "National ID / Passport / SSN", "IDENTITY", RequirementState.REQUIRED, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "dateOfBirth", "Date of Birth", "IDENTITY", RequirementState.OPTIONAL, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "gender", "Gender Demographics", "IDENTITY", RequirementState.OPTIONAL, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "nationality", "Country of Citizenship", "IDENTITY", RequirementState.OPTIONAL, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "maritalStatus", "Marital Status", "IDENTITY", RequirementState.DISABLED, order += 10));

        // CONTACT & EMERGENCY
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "personalPhone", "Personal Mobile Phone", "CONTACT", RequirementState.OPTIONAL, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "emergencyContactName", "Emergency Contact Name", "CONTACT", RequirementState.REQUIRED, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "emergencyContactPhone", "Emergency Contact Phone", "CONTACT", RequirementState.REQUIRED, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "emergencyContactRelation", "Emergency Relationship", "CONTACT", RequirementState.OPTIONAL, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "residentialAddress", "Residential Address", "CONTACT", RequirementState.OPTIONAL, order += 10));

        // INDUSTRY COMPLIANCE
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "licenseNumber", "Professional / Trade License #", "COMPLIANCE", RequirementState.OPTIONAL, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "licenseCategory", "License Category / Class", "COMPLIANCE", RequirementState.OPTIONAL, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "licenseExpiryDate", "License Expiration Date", "COMPLIANCE", RequirementState.OPTIONAL, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "safetyCertLevel", "Safety / Mining Certification Level", "COMPLIANCE", RequirementState.DISABLED, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "medicalClearanceDate", "Medical / Occupational Health Clearance Date", "COMPLIANCE", RequirementState.DISABLED, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "backgroundCheckStatus", "Vetting / Police Clearance Ref", "COMPLIANCE", RequirementState.OPTIONAL, order += 10));

        // EMPLOYMENT & ORG
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "departmentCode", "Department Code", "EMPLOYMENT", RequirementState.OPTIONAL, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "costCenter", "Cost Center Code", "EMPLOYMENT", RequirementState.OPTIONAL, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "workLocation", "Work Location / Facility Site", "EMPLOYMENT", RequirementState.OPTIONAL, order += 10));

        // FINANCIAL & TAX
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "taxIdentificationNumber", "Tax Identification Number (TIN)", "FINANCIAL", RequirementState.OPTIONAL, order += 10));
        list.add(createField(tenant, TargetEntity.EMPLOYEE, "bankAccountDetails", "Bank Account Details", "FINANCIAL", RequirementState.OPTIONAL, order += 10));

        // USER FIELDS CATALOG
        list.add(createField(tenant, TargetEntity.USER, "nationalId", "National ID / SSN", "IDENTITY", RequirementState.OPTIONAL, 10));
        list.add(createField(tenant, TargetEntity.USER, "taxIdentificationNumber", "Tax Identification Number (TIN)", "FINANCIAL", RequirementState.OPTIONAL, 20));
        list.add(createField(tenant, TargetEntity.USER, "phoneNumber", "Mobile Phone Number", "CONTACT", RequirementState.OPTIONAL, 30));

        return list;
    }

    private TenantOnboardingField createField(Tenant tenant, TargetEntity target, String key, String label, String category, RequirementState state, int displayOrder) {
        TenantOnboardingField f = new TenantOnboardingField();
        f.setTenant(tenant);
        f.setTargetEntity(target);
        f.setFieldKey(key);
        f.setFieldLabel(label);
        f.setFieldCategory(category);
        f.setRequirementState(state);
        f.setDisplayOrder(displayOrder);
        return f;
    }

    private TenantOnboardingFieldDto mapToDto(TenantOnboardingField entity) {
        TenantOnboardingFieldDto dto = new TenantOnboardingFieldDto();
        dto.setId(entity.getId());
        if (entity.getTenant() != null) {
            dto.setTenantId(entity.getTenant().getId());
        }
        dto.setTargetEntity(entity.getTargetEntity());
        dto.setFieldKey(entity.getFieldKey());
        dto.setFieldLabel(entity.getFieldLabel());
        dto.setFieldCategory(entity.getFieldCategory());
        dto.setRequirementState(entity.getRequirementState());
        dto.setDisplayOrder(entity.getDisplayOrder());
        dto.setHelpText(entity.getHelpText());
        return dto;
    }

    private void assertAccess(Long tenantId) {
        if (tenantSecurityService.isGlobalAdmin()) {
            return;
        }
        Long currentTenantId = tenantSecurityService.getCurrentTenantId();
        if (currentTenantId == null || !currentTenantId.equals(tenantId)) {
            throw new RuntimeException("Access denied: Tenant ID " + tenantId);
        }
    }
}
