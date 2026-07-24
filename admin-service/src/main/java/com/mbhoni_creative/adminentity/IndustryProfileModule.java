package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "industry_profile_modules",
    uniqueConstraints = @UniqueConstraint(columnNames = {"industry_profile_id", "module_id"})
)
public class IndustryProfileModule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "industry_profile_id", nullable = false)
    private IndustryProfile industryProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private PlatformModule module;

    private boolean enabledByDefault = true;

    public Long getId() { return id; }

    public IndustryProfile getIndustryProfile() { return industryProfile; }
    public void setIndustryProfile(IndustryProfile industryProfile) { this.industryProfile = industryProfile; }

    public PlatformModule getModule() { return module; }
    public void setModule(PlatformModule module) { this.module = module; }

    public boolean isEnabledByDefault() { return enabledByDefault; }
    public void setEnabledByDefault(boolean enabledByDefault) { this.enabledByDefault = enabledByDefault; }
}