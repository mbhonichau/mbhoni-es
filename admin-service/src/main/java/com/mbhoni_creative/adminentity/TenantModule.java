package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "tenant_modules",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "module_id"})
)
public class TenantModule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private PlatformModule module;

    private boolean enabled = true;

    public Long getId() { return id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public PlatformModule getModule() { return module; }
    public void setModule(PlatformModule module) { this.module = module; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}