package com.mbhoni_creative.adminservice;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_metrics")
public class TenantMetric {

    @Id
    private String tenantId;

    @Column(name = "`usage`")
    private String usage;

    private LocalDateTime timestamp;

    // Getters & Setters
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getUsage() { return usage; }
    public void setUsage(String usage) { this.usage = usage; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}