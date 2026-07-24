package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;

@Entity
@Table(name = "lookup_codes")
public class LookupCode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private LookupCategory category;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String value;

    @Column(length = 20)
    private String locale = "en_US";

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    private boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    public Long getId() { return id; }

    public LookupCategory getCategory() { return category; }
    public void setCategory(LookupCategory category) { this.category = category; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
