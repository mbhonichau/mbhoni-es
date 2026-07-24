package com.mbhoni_creative.admindto;

import com.mbhoni_creative.adminentity.SubscriptionStatus;

public class TenantSubscriptionView {

    private Long tenantId;
    private String tenantName;
    private boolean tenantActive;

    private String planName;
    private String planCode;
    private SubscriptionStatus subscriptionStatus;

    private long currentUsers;
    private Integer maxUsers;
    private String industryProfileName;

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

    public boolean isTenantActive() {
        return tenantActive;
    }

    public void setTenantActive(boolean tenantActive) {
        this.tenantActive = tenantActive;
    }

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getPlanCode() {
        return planCode;
    }

    public void setPlanCode(String planCode) {
        this.planCode = planCode;
    }

    public SubscriptionStatus getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public void setSubscriptionStatus(SubscriptionStatus subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public long getCurrentUsers() {
        return currentUsers;
    }

    public void setCurrentUsers(long currentUsers) {
        this.currentUsers = currentUsers;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public boolean hasSubscription() {
        return planName != null;
    }

    public boolean isUnlimitedUsers() {
        return maxUsers == null;
    }

    public boolean isAtUserLimit() {
        return maxUsers != null && currentUsers >= maxUsers;
    }

    public boolean isNearUserLimit() {
        return maxUsers != null && currentUsers < maxUsers && currentUsers >= Math.ceil(maxUsers * 0.8);
    }

    public String getUserLimitLabel() {
        if (maxUsers == null) {
            return currentUsers + " / Unlimited";
        }

        return currentUsers + " / " + maxUsers;
    }
    public String getIndustryProfileName() {
        return industryProfileName;
    }

    public void setIndustryProfileName(String industryProfileName) {
        this.industryProfileName = industryProfileName;
    }
    
}