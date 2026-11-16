package com.mbhoni_creative.adminentity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private TenantSubscription subscription;

    @Column(nullable = false, unique = true, length = 80)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    private LocalDate issueDate;
    private LocalDate dueDate;

    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal taxAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal amountPaid = BigDecimal.ZERO;
    @Column(unique = true)
    private String stripeInvoiceId;
    private String currency;
    private BigDecimal providerAmountRemaining;
    @Column(length = 2048)
    private String hostedInvoiceUrl;
    @Column(columnDefinition = "LONGTEXT")
    private String stripeInvoiceJson;
    public String getStripeInvoiceId() { return stripeInvoiceId; }
    public void setStripeInvoiceId(String value) { stripeInvoiceId = value; }
    public String getCurrency() { return currency; }
    public void setCurrency(String value) { currency = value; }
    public BigDecimal getProviderAmountRemaining() { return providerAmountRemaining; }
    public void setProviderAmountRemaining(BigDecimal value) { providerAmountRemaining = value; }
    public String getHostedInvoiceUrl() { return hostedInvoiceUrl; }
    public void setHostedInvoiceUrl(String value) { hostedInvoiceUrl = value; }
    public String getStripeInvoiceJson() { return stripeInvoiceJson; }
    public void setStripeInvoiceJson(String value) { stripeInvoiceJson = value; }

    @Column(length = 500)
    private String notes;

    public Long getId() { return id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public TenantSubscription getSubscription() { return subscription; }
    public void setSubscription(TenantSubscription subscription) { this.subscription = subscription; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal != null ? subtotal : BigDecimal.ZERO; }

    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount != null ? taxAmount : BigDecimal.ZERO; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO; }

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid != null ? amountPaid : BigDecimal.ZERO; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public BigDecimal getBalanceDue() {
        if (stripeInvoiceId != null) return providerAmountRemaining;
        return totalAmount.subtract(amountPaid);
    }
}
