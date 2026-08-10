package com.mbhoni_creative.admindto;

import java.util.List;

public class OnboardingSchemaResponse {

    private Long tenantId;
    private String tenantName;
    private String industryProfileName;
    private List<TenantOnboardingFieldDto> userFields;
    private List<TenantOnboardingFieldDto> employeeFields;

    // ================= GETTERS & SETTERS =================

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getIndustryProfileName() { return industryProfileName; }
    public void setIndustryProfileName(String industryProfileName) { this.industryProfileName = industryProfileName; }

    public List<TenantOnboardingFieldDto> getUserFields() { return userFields; }
    public void setUserFields(List<TenantOnboardingFieldDto> userFields) { this.userFields = userFields; }

    public List<TenantOnboardingFieldDto> getEmployeeFields() { return employeeFields; }
    public void setEmployeeFields(List<TenantOnboardingFieldDto> employeeFields) { this.employeeFields = employeeFields; }
}
