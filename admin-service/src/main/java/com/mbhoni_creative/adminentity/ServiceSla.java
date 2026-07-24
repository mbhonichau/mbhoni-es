package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "service_slas")
public class ServiceSla extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_catalog_id", nullable = false)
    private ServiceCatalog serviceCatalog;

    @Column(nullable = false, length = 50)
    private String tier = "STANDARD"; // BASIC, STANDARD, ENTERPRISE

    @Column(name = "target_uptime_percentage", precision = 5, scale = 2)
    private BigDecimal targetUptimePercentage = new BigDecimal("99.90");

    @Column(name = "max_latency_ms")
    private Integer maxLatencyMs = 500;

    @Column(name = "support_response_hours")
    private Integer supportResponseHours = 24;

    public Long getId() { return id; }

    public ServiceCatalog getServiceCatalog() { return serviceCatalog; }
    public void setServiceCatalog(ServiceCatalog serviceCatalog) { this.serviceCatalog = serviceCatalog; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public BigDecimal getTargetUptimePercentage() { return targetUptimePercentage; }
    public void setTargetUptimePercentage(BigDecimal targetUptimePercentage) { this.targetUptimePercentage = targetUptimePercentage; }

    public Integer getMaxLatencyMs() { return maxLatencyMs; }
    public void setMaxLatencyMs(Integer maxLatencyMs) { this.maxLatencyMs = maxLatencyMs; }

    public Integer getSupportResponseHours() { return supportResponseHours; }
    public void setSupportResponseHours(Integer supportResponseHours) { this.supportResponseHours = supportResponseHours; }
}
