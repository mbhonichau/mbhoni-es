package com.mbhoni_creative.contentmanagementservice;

import jakarta.persistence.*;

@Entity
@Table(name = "tenant_content")
public class TenantContent {

    @Id
    private String tenantId;

    private String about;
    private String media;
    private String products;

    // === GETTERS & SETTERS ===
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }

    public String getMedia() { return media; }
    public void setMedia(String media) { this.media = media; }

    public String getProducts() { return products; }
    public void setProducts(String products) { this.products = products; }
}