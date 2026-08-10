package com.mbhoni_creative.admindto;

import com.mbhoni_creative.adminentity.RequirementState;
import com.mbhoni_creative.adminentity.TargetEntity;

public class TenantOnboardingFieldDto {

    private Long id;
    private Long tenantId;
    private TargetEntity targetEntity = TargetEntity.EMPLOYEE;
    private String fieldKey;
    private String fieldLabel;
    private String fieldCategory = "IDENTITY";
    private RequirementState requirementState = RequirementState.OPTIONAL;
    private Integer displayOrder = 0;
    private String helpText;

    // ================= GETTERS & SETTERS =================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public TargetEntity getTargetEntity() { return targetEntity; }
    public void setTargetEntity(TargetEntity targetEntity) { this.targetEntity = targetEntity; }

    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }

    public String getFieldLabel() { return fieldLabel; }
    public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }

    public String getFieldCategory() { return fieldCategory; }
    public void setFieldCategory(String fieldCategory) { this.fieldCategory = fieldCategory; }

    public RequirementState getRequirementState() { return requirementState; }
    public void setRequirementState(RequirementState requirementState) { this.requirementState = requirementState; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public String getHelpText() { return helpText; }
    public void setHelpText(String helpText) { this.helpText = helpText; }
}
