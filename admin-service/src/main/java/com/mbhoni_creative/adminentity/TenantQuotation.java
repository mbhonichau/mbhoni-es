package com.mbhoni_creative.adminentity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name = "tenant_quotations")
public class TenantQuotation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 80)
    private String quotationNumber;

    @Column(length = 255)
    private String customerName;

    @Column(length = 255)
    private String customerEmail;

    @Column(length = 50)
    private String customerPhone;

    @Column(length = 500)
    private String customerAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private QuotationStatus status = QuotationStatus.DRAFT;

    private LocalDate issueDate = LocalDate.now();
    private LocalDate validUntilDate = LocalDate.now().plusDays(14);

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(length = 1000)
    private String notes;

    private Long convertedInvoiceId;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TenantQuotationItem> items = new ArrayList<>();

    public void addItem(TenantQuotationItem item) {
        item.setQuotation(this);
        item.calculateTotal();
        items.add(item);
        recalculateTotals();
    }

    public void recalculateTotals() {
        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (TenantQuotationItem item : items) {
            if (item.getQuantity() != null && item.getUnitPrice() != null) {
                BigDecimal lineBase = item.getQuantity().multiply(item.getUnitPrice());
                sub = sub.add(lineBase);
                if (item.getTaxRate() != null && item.getTaxRate().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal lineTax = lineBase.multiply(item.getTaxRate().divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP));
                    tax = tax.add(lineTax);
                }
            }
        }
        this.subtotal = sub.setScale(2, java.math.RoundingMode.HALF_UP);
        this.taxAmount = tax.setScale(2, java.math.RoundingMode.HALF_UP);
        this.totalAmount = this.subtotal.add(this.taxAmount).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    public Long getId() { return id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getQuotationNumber() { return quotationNumber; }
    public void setQuotationNumber(String quotationNumber) { this.quotationNumber = quotationNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }

    public QuotationStatus getStatus() { return status; }
    public void setStatus(QuotationStatus status) { this.status = status; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getValidUntilDate() { return validUntilDate; }
    public void setValidUntilDate(LocalDate validUntilDate) { this.validUntilDate = validUntilDate; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getConvertedInvoiceId() { return convertedInvoiceId; }
    public void setConvertedInvoiceId(Long convertedInvoiceId) { this.convertedInvoiceId = convertedInvoiceId; }

    public List<TenantQuotationItem> getItems() { return items; }
    public void setItems(List<TenantQuotationItem> items) { this.items = items; }
}
