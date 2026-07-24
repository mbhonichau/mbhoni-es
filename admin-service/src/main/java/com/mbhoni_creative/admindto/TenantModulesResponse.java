package com.mbhoni_creative.admindto;

import java.util.ArrayList;
import java.util.List;

public class TenantModulesResponse {

    private Long tenantId;
    private String tenantName;
    private List<TenantModuleView> modules = new ArrayList<>();

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public List<TenantModuleView> getModules() { return modules; }
    public void setModules(List<TenantModuleView> modules) { this.modules = modules; }
}