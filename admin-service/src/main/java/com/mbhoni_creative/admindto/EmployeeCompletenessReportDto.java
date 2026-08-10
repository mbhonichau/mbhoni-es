package com.mbhoni_creative.admindto;

import java.util.List;

public class EmployeeCompletenessReportDto {
    private Long employeeId;
    private String employeeNumber;
    private String employeeName;
    private Long tenantId;
    private boolean isComplete;
    private List<String> missingFieldKeys;
    private List<String> missingFieldLabels;

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public boolean isComplete() { return isComplete; }
    public void setComplete(boolean complete) { isComplete = complete; }

    public List<String> getMissingFieldKeys() { return missingFieldKeys; }
    public void setMissingFieldKeys(List<String> missingFieldKeys) { this.missingFieldKeys = missingFieldKeys; }

    public List<String> getMissingFieldLabels() { return missingFieldLabels; }
    public void setMissingFieldLabels(List<String> missingFieldLabels) { this.missingFieldLabels = missingFieldLabels; }
}
