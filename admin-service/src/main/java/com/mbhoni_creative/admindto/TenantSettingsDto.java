package com.mbhoni_creative.admindto;

public class TenantSettingsDto {

    private Long id;
    private Long tenantId;
    private String tenantName;

    // Security & Authentication Governance
    private boolean mfaEnforced = false;
    private boolean ssoEnforced = false;
    private Integer passwordRotationDays = 90;
    private Integer maxFailedLoginAttempts = 5;
    private Integer sessionTimeoutMinutes = 30;
    private boolean ipWhitelistEnabled = false;
    private String allowedIpRanges;

    // Audit, Logging & Compliance Governance
    private boolean auditExtendedLogging = false;
    private Integer dataRetentionDays = 365;
    private boolean gdprComplianceMode = false;
    private boolean complianceExportEnabled = true;

    // API & Integration Administration
    private boolean apiAccessEnabled = true;
    private boolean webhookEventsEnabled = false;
    private Integer rateLimitPerMinute = 1000;
    private boolean customDomainEnabled = false;

    // Workspace Operations & Self-Service
    private boolean maintenanceMode = false;
    private boolean selfRegistrationAllowed = false;
    private boolean allowCustomRoles = true;
    private boolean multiCurrencyEnabled = false;
    private Integer maxFileUploadMb = 50;

    // Subscription & Billing Entitlement
    private boolean overageAllowed = false;
    private boolean autoRenewalEnabled = true;

    // ================= GETTERS & SETTERS =================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getPasswordRotationDays() {
        return passwordRotationDays;
    }

    public void setPasswordRotationDays(Integer passwordRotationDays) {
        this.passwordRotationDays = passwordRotationDays;
    }

    public Integer getMaxFailedLoginAttempts() {
        return maxFailedLoginAttempts;
    }

    public void setMaxFailedLoginAttempts(Integer maxFailedLoginAttempts) {
        this.maxFailedLoginAttempts = maxFailedLoginAttempts;
    }

    public Integer getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }

    public void setSessionTimeoutMinutes(Integer sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    public boolean isIpWhitelistEnabled() {
        return ipWhitelistEnabled;
    }

    public void setIpWhitelistEnabled(boolean ipWhitelistEnabled) {
        this.ipWhitelistEnabled = ipWhitelistEnabled;
    }

    public String getAllowedIpRanges() {
        return allowedIpRanges;
    }

    public void setAllowedIpRanges(String allowedIpRanges) {
        this.allowedIpRanges = allowedIpRanges;
    }

    public boolean isAuditExtendedLogging() {
        return auditExtendedLogging;
    }

    public void setAuditExtendedLogging(boolean auditExtendedLogging) {
        this.auditExtendedLogging = auditExtendedLogging;
    }

    public Integer getDataRetentionDays() {
        return dataRetentionDays;
    }

    public void setDataRetentionDays(Integer dataRetentionDays) {
        this.dataRetentionDays = dataRetentionDays;
    }

    public boolean isGdprComplianceMode() {
        return gdprComplianceMode;
    }

    public void setGdprComplianceMode(boolean gdprComplianceMode) {
        this.gdprComplianceMode = gdprComplianceMode;
    }

    public boolean isComplianceExportEnabled() {
        return complianceExportEnabled;
    }

    public void setComplianceExportEnabled(boolean complianceExportEnabled) {
        this.complianceExportEnabled = complianceExportEnabled;
    }

    public boolean isApiAccessEnabled() {
        return apiAccessEnabled;
    }

    public void setApiAccessEnabled(boolean apiAccessEnabled) {
        this.apiAccessEnabled = apiAccessEnabled;
    }

    public boolean isWebhookEventsEnabled() {
        return webhookEventsEnabled;
    }

    public void setWebhookEventsEnabled(boolean webhookEventsEnabled) {
        this.webhookEventsEnabled = webhookEventsEnabled;
    }

    public Integer getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(Integer rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public boolean isCustomDomainEnabled() {
        return customDomainEnabled;
    }

    public void setCustomDomainEnabled(boolean customDomainEnabled) {
        this.customDomainEnabled = customDomainEnabled;
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean maintenanceMode) {
        this.maintenanceMode = maintenanceMode;
    }

    public boolean isSelfRegistrationAllowed() {
        return selfRegistrationAllowed;
    }

    public void setSelfRegistrationAllowed(boolean selfRegistrationAllowed) {
        this.selfRegistrationAllowed = selfRegistrationAllowed;
    }

    public boolean isAllowCustomRoles() {
        return allowCustomRoles;
    }

    public void setAllowCustomRoles(boolean allowCustomRoles) {
        this.allowCustomRoles = allowCustomRoles;
    }

    public boolean isMultiCurrencyEnabled() {
        return multiCurrencyEnabled;
    }

    public void setMultiCurrencyEnabled(boolean multiCurrencyEnabled) {
        this.multiCurrencyEnabled = multiCurrencyEnabled;
    }

    public Integer getMaxFileUploadMb() {
        return maxFileUploadMb;
    }

    public void setMaxFileUploadMb(Integer maxFileUploadMb) {
        this.maxFileUploadMb = maxFileUploadMb;
    }

    public boolean isOverageAllowed() {
        return overageAllowed;
    }

    public void setOverageAllowed(boolean overageAllowed) {
        this.overageAllowed = overageAllowed;
    }

    public boolean isAutoRenewalEnabled() {
        return autoRenewalEnabled;
    }

    public void setAutoRenewalEnabled(boolean autoRenewalEnabled) {
        this.autoRenewalEnabled = autoRenewalEnabled;
    }
}
