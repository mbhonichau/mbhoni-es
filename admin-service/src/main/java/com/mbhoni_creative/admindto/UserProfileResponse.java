package com.mbhoni_creative.admindto;

import java.util.Map;
import java.util.Set;

public class UserProfileResponse {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String avatarUrl;
    private String department;
    private String jobTitle;
    private String timeZone;
    private String authProvider;
    private boolean globalAdmin;
    private boolean active;
    private boolean passwordChangeRequired;
    private boolean mfaEnforced;
    private boolean ssoEnforced;
    private boolean apiAccessAllowed;
    private boolean auditExtended;
    private Long tenantId;
    private String tenantName;
    private Set<String> roles;
    private Set<String> permissions;
    private Map<String, Boolean> processToggles;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public boolean isGlobalAdmin() {
        return globalAdmin;
    }

    public void setGlobalAdmin(boolean globalAdmin) {
        this.globalAdmin = globalAdmin;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isPasswordChangeRequired() {
        return passwordChangeRequired;
    }

    public void setPasswordChangeRequired(boolean passwordChangeRequired) {
        this.passwordChangeRequired = passwordChangeRequired;
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

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }

    public Map<String, Boolean> getProcessToggles() {
        return processToggles;
    }

    public void setProcessToggles(Map<String, Boolean> processToggles) {
        this.processToggles = processToggles;
    }
}
