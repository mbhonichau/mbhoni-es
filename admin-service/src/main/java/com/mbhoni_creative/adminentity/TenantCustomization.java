package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;

@Entity
@Table(name = "tenant_customizations")
public class TenantCustomization extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    private String displayName;

    @Column(length = 500)
    private String logoUrl;

    private String primaryColor;
    private String secondaryColor;
    private String supportEmail;
    private String supportPhone;
    private String customDomain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ThemeMode themeMode = ThemeMode.SYSTEM;

    private String footerText;
    private String timezone;
    private String country;
    private String currency;
    private String language;

    private boolean active = true;

    public Long getId() { return id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getPrimaryColor() { return primaryColor; }
    public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }

    public String getSecondaryColor() { return secondaryColor; }
    public void setSecondaryColor(String secondaryColor) { this.secondaryColor = secondaryColor; }

    public String getSupportEmail() { return supportEmail; }
    public void setSupportEmail(String supportEmail) { this.supportEmail = supportEmail; }

    public String getSupportPhone() { return supportPhone; }
    public void setSupportPhone(String supportPhone) { this.supportPhone = supportPhone; }

    public String getCustomDomain() { return customDomain; }
    public void setCustomDomain(String customDomain) { this.customDomain = customDomain; }

    public ThemeMode getThemeMode() { return themeMode; }
    public void setThemeMode(ThemeMode themeMode) {
        this.themeMode = themeMode != null ? themeMode : ThemeMode.SYSTEM;
    }

    public String getFooterText() { return footerText; }
    public void setFooterText(String footerText) { this.footerText = footerText; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}