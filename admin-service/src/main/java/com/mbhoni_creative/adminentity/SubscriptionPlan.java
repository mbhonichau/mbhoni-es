package com.mbhoni_creative.adminentity;

import java.math.BigDecimal;

import jakarta.persistence.*;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    private BigDecimal monthlyPrice = BigDecimal.ZERO;

    private BigDecimal annualPrice = BigDecimal.ZERO;

    private Integer maxUsers;

    private Integer maxStorageMb;

    private boolean apiAccessEnabled;

    private boolean brandingEnabled;

    private boolean customDomainEnabled;

    private boolean active = true;

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code != null ? code.trim().toUpperCase() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getMonthlyPrice() {
        return monthlyPrice;
    }

    public void setMonthlyPrice(BigDecimal monthlyPrice) {
        this.monthlyPrice = monthlyPrice != null ? monthlyPrice : BigDecimal.ZERO;
    }

    public BigDecimal getAnnualPrice() {
        return annualPrice;
    }

    public void setAnnualPrice(BigDecimal annualPrice) {
        this.annualPrice = annualPrice != null ? annualPrice : BigDecimal.ZERO;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public Integer getMaxStorageMb() {
        return maxStorageMb;
    }

    public void setMaxStorageMb(Integer maxStorageMb) {
        this.maxStorageMb = maxStorageMb;
    }

    public boolean isApiAccessEnabled() {
        return apiAccessEnabled;
    }

    public void setApiAccessEnabled(boolean apiAccessEnabled) {
        this.apiAccessEnabled = apiAccessEnabled;
    }

    public boolean isBrandingEnabled() {
        return brandingEnabled;
    }

    public void setBrandingEnabled(boolean brandingEnabled) {
        this.brandingEnabled = brandingEnabled;
    }

    public boolean isCustomDomainEnabled() {
        return customDomainEnabled;
    }

    public void setCustomDomainEnabled(boolean customDomainEnabled) {
        this.customDomainEnabled = customDomainEnabled;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}