package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;

@Entity
@Table(name = "tenant_settings")
public class TenantSettings extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    // ================= SECURITY & AUTHENTICATION GOVERNANCE =================
    @Column(name = "mfa_enforced", nullable = false)
    private boolean mfaEnforced = false;

    @Column(name = "sso_enforced", nullable = false)
    private boolean ssoEnforced = false;

    @Column(name = "password_rotation_days", nullable = false)
    private Integer passwordRotationDays = 90;

    @Column(name = "max_failed_login_attempts", nullable = false)
    private Integer maxFailedLoginAttempts = 5;

    @Column(name = "session_timeout_minutes", nullable = false)
    private Integer sessionTimeoutMinutes = 30;

    @Column(name = "ip_whitelist_enabled", nullable = false)
    private boolean ipWhitelistEnabled = false;

    @Column(name = "allowed_ip_ranges", length = 1000)
    private String allowedIpRanges;

    // ================= AUDIT, LOGGING & COMPLIANCE GOVERNANCE =================
    @Column(name = "audit_extended_logging", nullable = false)
    private boolean auditExtendedLogging = false;

    @Column(name = "data_retention_days", nullable = false)
    private Integer dataRetentionDays = 365;

    @Column(name = "gdpr_compliance_mode", nullable = false)
    private boolean gdprComplianceMode = false;

    @Column(name = "compliance_export_enabled", nullable = false)
    private boolean complianceExportEnabled = true;

    // ================= API & INTEGRATION ADMINISTRATION =================
    @Column(name = "api_access_enabled", nullable = false)
    private boolean apiAccessEnabled = true;

    @Column(name = "webhook_events_enabled", nullable = false)
    private boolean webhookEventsEnabled = false;

    @Column(name = "rate_limit_per_minute", nullable = false)
    private Integer rateLimitPerMinute = 1000;

    @Column(name = "custom_domain_enabled", nullable = false)
    private boolean customDomainEnabled = false;

    // ================= WORKSPACE OPERATIONS & SELF-SERVICE =================
    @Column(name = "maintenance_mode", nullable = false)
    private boolean maintenanceMode = false;

    @Column(name = "self_registration_allowed", nullable = false)
    private boolean selfRegistrationAllowed = false;

    @Column(name = "allow_custom_roles", nullable = false)
    private boolean allowCustomRoles = true;

    @Column(name = "multi_currency_enabled", nullable = false)
    private boolean multiCurrencyEnabled = false;

    @Column(name = "max_file_upload_mb", nullable = false)
    private Integer maxFileUploadMb = 50;

    // ================= SUBSCRIPTION & BILLING ENTITLEMENT =================
    @Column(name = "overage_allowed", nullable = false)
    private boolean overageAllowed = false;

    @Column(name = "auto_renewal_enabled", nullable = false)
    private boolean autoRenewalEnabled = true;

    // ================= GETTERS & SETTERS =================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
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
