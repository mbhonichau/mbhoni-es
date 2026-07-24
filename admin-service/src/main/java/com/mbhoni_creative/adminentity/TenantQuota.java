package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;

@Entity
@Table(name = "tenant_quotas")
public class TenantQuota extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    @Column(name = "max_users")
    private Integer maxUsers = 50;

    @Column(name = "max_api_requests_per_minute")
    private Integer maxApiRequestsPerMinute = 1000;

    @Column(name = "max_storage_gb")
    private Integer maxStorageGb = 100;

    @Column(name = "custom_quotas_json", columnDefinition = "TEXT")
    private String customQuotasJson;

    public Long getId() { return id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public Integer getMaxUsers() { return maxUsers; }
    public void setMaxUsers(Integer maxUsers) { this.maxUsers = maxUsers; }

    public Integer getMaxApiRequestsPerMinute() { return maxApiRequestsPerMinute; }
    public void setMaxApiRequestsPerMinute(Integer maxApiRequestsPerMinute) { this.maxApiRequestsPerMinute = maxApiRequestsPerMinute; }

    public Integer getMaxStorageGb() { return maxStorageGb; }
    public void setMaxStorageGb(Integer maxStorageGb) { this.maxStorageGb = maxStorageGb; }

    public String getCustomQuotasJson() { return customQuotasJson; }
    public void setCustomQuotasJson(String customQuotasJson) { this.customQuotasJson = customQuotasJson; }
}
