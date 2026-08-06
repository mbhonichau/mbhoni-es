package com.mbhoni_creative.adminentity;

import java.math.BigDecimal;
import jakarta.persistence.*;

@Entity
@Table(name = "tenant_quotation_items")
public class TenantQuotationItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quotation_id", nullable = false)
    private TenantQuotation quotation;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate = new BigDecimal("15.00");

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    public void calculateTotal() {
        if (quantity == null) quantity = BigDecimal.ONE;
        if (unitPrice == null) unitPrice = BigDecimal.ZERO;
        BigDecimal base = quantity.multiply(unitPrice);
        if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal taxMultiplier = BigDecimal.ONE.add(taxRate.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP));
            this.totalAmount = base.multiply(taxMultiplier).setScale(2, java.math.RoundingMode.HALF_UP);
        } else {
            this.totalAmount = base.setScale(2, java.math.RoundingMode.HALF_UP);
        }
    }

    public Long getId() { return id; }

    public TenantQuotation getQuotation() { return quotation; }
    public void setQuotation(TenantQuotation quotation) { this.quotation = quotation; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getTaxRate() { return taxRate; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
