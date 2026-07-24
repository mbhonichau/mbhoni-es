package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;

@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_profile_id")
    private IndustryProfile industryProfile;

    private boolean active = true;

    @Column(name = "tenant_code", unique = true, length = 100)
    private String tenantCode;

    @Column(name = "isolation_strategy", length = 50)
    private String isolationStrategy = "SHARED_DATABASE";

    @Column(name = "primary_region", length = 50)
    private String primaryRegion = "us-east-1";

    @Column(length = 50)
    private String status = "ACTIVE";

    @Column(name = "data_retention_days")
    private Integer dataRetentionDays = 365;

    // ================= GETTERS/SETTERS =================

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public IndustryProfile getIndustryProfile() {
        return industryProfile;
    }

    public void setIndustryProfile(IndustryProfile industryProfile) {
        this.industryProfile = industryProfile;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public String getIsolationStrategy() {
        return isolationStrategy;
    }

    public void setIsolationStrategy(String isolationStrategy) {
        this.isolationStrategy = isolationStrategy;
    }

    public String getPrimaryRegion() {
        return primaryRegion;
    }

    public void setPrimaryRegion(String primaryRegion) {
        this.primaryRegion = primaryRegion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDataRetentionDays() {
        return dataRetentionDays;
    }

    public void setDataRetentionDays(Integer dataRetentionDays) {
        this.dataRetentionDays = dataRetentionDays;
    }
}