package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;

@Entity
@Table(name = "industry_profile_fields", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"industry_profile_id", "target_entity", "field_key"})
})
public class IndustryProfileField extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_profile_id", nullable = false)
    private IndustryProfile industryProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_entity", nullable = false, length = 30)
    private TargetEntity targetEntity = TargetEntity.EMPLOYEE;

    @Column(name = "field_key", nullable = false, length = 80)
    private String fieldKey;

    @Column(name = "field_label", nullable = false, length = 120)
    private String fieldLabel;

    @Column(name = "field_category", nullable = false, length = 50)
    private String fieldCategory = "IDENTITY";

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_state", nullable = false, length = 30)
    private RequirementState requirementState = RequirementState.OPTIONAL;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    // ================= GETTERS & SETTERS =================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public IndustryProfile getIndustryProfile() { return industryProfile; }
    public void setIndustryProfile(IndustryProfile industryProfile) { this.industryProfile = industryProfile; }

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
}
