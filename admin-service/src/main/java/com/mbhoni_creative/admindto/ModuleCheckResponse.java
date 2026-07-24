package com.mbhoni_creative.admindto;

public class ModuleCheckResponse {

    private Long tenantId;
    private String moduleCode;
    private boolean enabled;

    public ModuleCheckResponse() {
    }

    public ModuleCheckResponse(Long tenantId, String moduleCode, boolean enabled) {
        this.tenantId = tenantId;
        this.moduleCode = moduleCode;
        this.enabled = enabled;
    }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getModuleCode() { return moduleCode; }
    public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}