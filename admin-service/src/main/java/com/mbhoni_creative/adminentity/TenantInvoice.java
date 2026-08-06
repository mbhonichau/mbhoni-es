package com.mbhoni_creative.adminentity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.*;

@Entity
@Table(name = "tenant_invoices")
public class TenantInvoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 80)
    private String invoiceNumber;

    @Column(length = 255)
    private String customerName;

    @Column(length = 255)
    private String customerEmail;

    @Column(length = 50)
    private String customerPhone;

    @Column(length = 500)
    private String customerAddress;

    @Column(length = 80)
    private String customerTaxNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    private LocalDate issueDate = LocalDate.now();
    private LocalDate dueDate = LocalDate.now().plusDays(30);

    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(length = 1000)
    private String notes;

    @Column(length = 500)
    private String paymentTerms = "Payment due within 30 days of issue.";

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TenantInvoiceItem> items = new ArrayList<>();

    public void addItem(TenantInvoiceItem item) {
        item.setInvoice(this);
        item.calculateTotal();
        items.add(item);
        recalculateTotals();
    }

    public void recalculateTotals() {
        BigDecimal sub = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (TenantInvoiceItem item : items) {
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

    public BigDecimal getBalanceDue() {
        BigDecimal total = totalAmount != null ? totalAmount : BigDecimal.ZERO;
        BigDecimal paid = amountPaid != null ? amountPaid : BigDecimal.ZERO;
        BigDecimal bal = total.subtract(paid);
        return bal.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : bal;
    }

    public Long getId() { return id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }

    public String getCustomerTaxNumber() { return customerTaxNumber; }
    public void setCustomerTaxNumber(String customerTaxNumber) { this.customerTaxNumber = customerTaxNumber; }

    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(String paymentTerms) { this.paymentTerms = paymentTerms; }

    public List<TenantInvoiceItem> getItems() { return items; }
    public void setItems(List<TenantInvoiceItem> items) { this.items = items; }
}
