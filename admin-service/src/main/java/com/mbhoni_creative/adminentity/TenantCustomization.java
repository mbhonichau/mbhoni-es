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

    @Lob
    @Column(name = "payslip_template_html", columnDefinition = "LONGTEXT", length = 10485760)
    private String payslipTemplateHtml;

    @Lob
    @Column(name = "payslip_excel_template", columnDefinition = "LONGBLOB")
    private byte[] payslipExcelTemplate;

    @Column(name = "payslip_excel_file_name")
    private String payslipExcelFileName;

    @Column(name = "payslip_engine_type", length = 20)
    private String payslipEngineType = "EXCEL";

    @Lob
    @Column(name = "payslip_word_template", columnDefinition = "LONGBLOB")
    private byte[] payslipWordTemplate;

    @Column(name = "payslip_word_file_name")
    private String payslipWordFileName;

    @Column(name = "invoice_engine_type", length = 20)
    private String invoiceEngineType = "EXCEL";

    @Lob
    @Column(name = "invoice_excel_template", columnDefinition = "LONGBLOB")
    private byte[] invoiceExcelTemplate;

    @Column(name = "invoice_excel_file_name")
    private String invoiceExcelFileName;

    @Lob
    @Column(name = "invoice_word_template", columnDefinition = "LONGBLOB")
    private byte[] invoiceWordTemplate;

    @Column(name = "invoice_word_file_name")
    private String invoiceWordFileName;

    public Long getId() { return id; }

    public String getPayslipTemplateHtml() { return payslipTemplateHtml; }
    public void setPayslipTemplateHtml(String payslipTemplateHtml) { this.payslipTemplateHtml = payslipTemplateHtml; }

    public byte[] getPayslipExcelTemplate() { return payslipExcelTemplate; }
    public void setPayslipExcelTemplate(byte[] payslipExcelTemplate) { this.payslipExcelTemplate = payslipExcelTemplate; }

    public String getPayslipExcelFileName() { return payslipExcelFileName; }
    public void setPayslipExcelFileName(String payslipExcelFileName) { this.payslipExcelFileName = payslipExcelFileName; }

    public String getPayslipEngineType() { return payslipEngineType != null ? payslipEngineType : "EXCEL"; }
    public void setPayslipEngineType(String payslipEngineType) { this.payslipEngineType = payslipEngineType; }

    public byte[] getPayslipWordTemplate() { return payslipWordTemplate; }
    public void setPayslipWordTemplate(byte[] payslipWordTemplate) { this.payslipWordTemplate = payslipWordTemplate; }

    public String getPayslipWordFileName() { return payslipWordFileName; }
    public void setPayslipWordFileName(String payslipWordFileName) { this.payslipWordFileName = payslipWordFileName; }

    public String getInvoiceEngineType() { return invoiceEngineType != null ? invoiceEngineType : "EXCEL"; }
    public void setInvoiceEngineType(String invoiceEngineType) { this.invoiceEngineType = invoiceEngineType; }

    public byte[] getInvoiceExcelTemplate() { return invoiceExcelTemplate; }
    public void setInvoiceExcelTemplate(byte[] invoiceExcelTemplate) { this.invoiceExcelTemplate = invoiceExcelTemplate; }

    public String getInvoiceExcelFileName() { return invoiceExcelFileName; }
    public void setInvoiceExcelFileName(String invoiceExcelFileName) { this.invoiceExcelFileName = invoiceExcelFileName; }

    public byte[] getInvoiceWordTemplate() { return invoiceWordTemplate; }
    public void setInvoiceWordTemplate(byte[] invoiceWordTemplate) { this.invoiceWordTemplate = invoiceWordTemplate; }

    public String getInvoiceWordFileName() { return invoiceWordFileName; }
    public void setInvoiceWordFileName(String invoiceWordFileName) { this.invoiceWordFileName = invoiceWordFileName; }

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