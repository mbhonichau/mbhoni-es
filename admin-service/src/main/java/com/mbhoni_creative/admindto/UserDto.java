package com.mbhoni_creative.admindto;

import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserDto {

    private Long id;

    private String username;

    @JsonIgnore // prevent accidental exposure
    private String password;

    private String email;

    private String department;

    private String jobTitle;

    private String timeZone;

    private boolean mfaEnforced;

    private boolean ssoEnforced;

    private boolean apiAccessAllowed = true;

    private boolean auditExtended;

    private Long tenantId;

    private String tenantName;

    private Set<String> roles;

    private Set<Long> roleIds = new HashSet<>();

    private boolean active;

    // ================= GETTERS & SETTERS =================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public boolean isMfaEnforced() {
        return mfaEnforced;
    }

    public void setMfaEnforced(boolean mfaEnforced) {
        this.mfaEnforced = mfaEnforced;
    }

    public boolean isSsoEnforced() {
        return ssoEnforced;
    }

    public void setSsoEnforced(boolean ssoEnforced) {
        this.ssoEnforced = ssoEnforced;
    }

    public boolean isApiAccessAllowed() {
        return apiAccessAllowed;
    }

    public void setApiAccessAllowed(boolean apiAccessAllowed) {
        this.apiAccessAllowed = apiAccessAllowed;
    }

    public boolean isAuditExtended() {
        return auditExtended;
    }

    public void setAuditExtended(boolean auditExtended) {
        this.auditExtended = auditExtended;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(Set<Long> roleIds) {
        this.roleIds = roleIds != null ? roleIds : new HashSet<>();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
