package com.mbhoni_creative.admindto;

import com.mbhoni_creative.adminentity.ThemeMode;

public class TenantBrandingResponse {

    private Long tenantId;
    private String tenantName;
    private String displayName;
    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;
    private String supportEmail;
    private String supportPhone;
    private String customDomain;
    private ThemeMode themeMode;
    private String footerText;
    private String timezone;
    private String country;
    private String currency;
    private String language;
    private boolean active;
    private boolean brandingAllowed;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public String getTenantName() { return tenantName; }
    public void setTenantName(String tenantName) { this.tenantName = tenantName; }

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
    public void setThemeMode(ThemeMode themeMode) { this.themeMode = themeMode; }

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

    public boolean isBrandingAllowed() { return brandingAllowed; }
    public void setBrandingAllowed(boolean brandingAllowed) { this.brandingAllowed = brandingAllowed; }
}