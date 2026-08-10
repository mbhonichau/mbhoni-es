package com.mbhoni_creative.admindto;

import com.mbhoni_creative.adminentity.SubscriptionStatus;

public class TenantFeatureResponse {

    private Long tenantId;
    private String tenantName;

    private String planCode;
    private String planName;
    private SubscriptionStatus subscriptionStatus;

    private Integer maxUsers;
    private Integer maxStorageMb;

    private boolean apiAccessEnabled;
    private boolean brandingEnabled;
    private boolean customDomainEnabled;

    private boolean activeSubscription;
    private boolean suspended;
    private boolean cancelled;

    private TenantSettingsDto adminSettings;

    public TenantSettingsDto getAdminSettings() { return adminSettings; }
    public void setAdminSettings(TenantSettingsDto adminSettings) { this.adminSettings = adminSettings; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public SubscriptionStatus getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) { this.subscriptionStatus = subscriptionStatus; }

    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }

    public Integer getMaxStorageMb() { return maxStorageMb; }
    public void setMaxStorageMb(Integer maxStorageMb) { this.maxStorageMb = maxStorageMb; }

    public boolean isApiAccessEnabled() { return apiAccessEnabled; }
    public void setApiAccessEnabled(boolean apiAccessEnabled) { this.apiAccessEnabled = apiAccessEnabled; }

    public boolean isBrandingEnabled() { return brandingEnabled; }
    public void setBrandingEnabled(boolean brandingEnabled) { this.brandingEnabled = brandingEnabled; }

    public boolean isCustomDomainEnabled() { return customDomainEnabled; }
    public void setCustomDomainEnabled(boolean customDomainEnabled) { this.customDomainEnabled = customDomainEnabled; }

    public boolean isActiveSubscription() { return activeSubscription; }
    public void setActiveSubscription(boolean activeSubscription) { this.activeSubscription = activeSubscription; }

    public boolean isSuspended() { return suspended; }
    public void setSuspended(boolean suspended) { this.suspended = suspended; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}