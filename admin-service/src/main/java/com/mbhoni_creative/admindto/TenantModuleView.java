package com.mbhoni_creative.admindto;

public class TenantModuleView {

    private Long moduleId;
    private String code;
    private String name;
    private String description;
    private String category;
    private boolean enabled;
    private boolean allowedByPlan;
    
    public Long getModuleId() { return moduleId; }
    public void setModuleId(Long moduleId) { this.moduleId = moduleId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isAllowedByPlan() {
        return allowedByPlan;
    }

    public void setAllowedByPlan(boolean allowedByPlan) {
        this.allowedByPlan = allowedByPlan;
    }
}