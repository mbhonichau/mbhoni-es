package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;

@Entity
@Table(name = "field_definitions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_tenant_section_field_key", columnNames = {"tenant_id", "section", "field_key"})
})
public class FieldDefinition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id", nullable = true)
    private Tenant tenant; // NULL = global default, applies to all tenants

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FieldSection section;

    @Column(name = "field_key", nullable = false, length = 100)
    private String fieldKey;

    @Column(nullable = false, length = 150)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private DataType dataType;

    @Column(name = "select_options", columnDefinition = "TEXT")
    private String selectOptions; // JSON array, only when data_type = SELECT

    @Column(name = "is_required", nullable = false)
    private boolean isRequired = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "validation_rule", length = 255)
    private String validationRule;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public FieldSection getSection() { return section; }
    public void setSection(FieldSection section) { this.section = section; }

    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public DataType getDataType() { return dataType; }
    public void setDataType(DataType dataType) { this.dataType = dataType; }

    public String getSelectOptions() { return selectOptions; }
    public void setSelectOptions(String selectOptions) { this.selectOptions = selectOptions; }

    public boolean isRequired() { return isRequired; }
    public void setRequired(boolean required) { isRequired = required; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public String getValidationRule() { return validationRule; }
    public void setValidationRule(String validationRule) { this.validationRule = validationRule; }
}
