package com.mbhoni_creative.adminservice.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.admindto.BillingDashboardView;
import com.mbhoni_creative.adminentity.BillingAccount;
import com.mbhoni_creative.adminentity.Invoice;
import com.mbhoni_creative.adminentity.InvoiceStatus;
import com.mbhoni_creative.adminentity.Payment;
import com.mbhoni_creative.adminentity.PaymentMethod;
import com.mbhoni_creative.adminentity.PaymentProvider;
import com.mbhoni_creative.adminentity.PaymentStatus;
import com.mbhoni_creative.adminentity.SubscriptionStatus;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantSubscription;
import com.mbhoni_creative.adminrepository.BillingAccountRepository;
import com.mbhoni_creative.adminrepository.InvoiceRepository;
import com.mbhoni_creative.adminrepository.PaymentRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.adminservice.BillingService;
import com.mbhoni_creative.adminservice.NotificationService;

@Service
public class BillingServiceImpl implements BillingService {

    private final BillingAccountRepository billingAccountRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final TenantRepository tenantRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final NotificationService notificationService;

    public BillingServiceImpl(
            BillingAccountRepository billingAccountRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            TenantRepository tenantRepository,
            TenantSubscriptionRepository tenantSubscriptionRepository,
            NotificationService notificationService) {

        this.billingAccountRepository = billingAccountRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.tenantRepository = tenantRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.notificationService = notificationService;
    }

    @Override
    public List<BillingAccount> getAllBillingAccounts() {
        return billingAccountRepository.findAll();
    }

    @Override
    public BillingAccount getBillingAccountById(Long id) {
        return billingAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Billing account not found"));
    }

    @Override
    @Transactional
    public BillingAccount saveOrUpdateBillingAccount(Long tenantId, BillingAccount source) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        BillingAccount account = billingAccountRepository.findByTenant(tenant)
                .orElseGet(BillingAccount::new);

        account.setTenant(tenant);
        account.setBillingName(source.getBillingName());
        account.setBillingEmail(source.getBillingEmail());
        account.setTaxNumber(source.getTaxNumber());
        account.setAddressLine1(source.getAddressLine1());
        account.setAddressLine2(source.getAddressLine2());
        account.setCity(source.getCity());
        account.setProvince(source.getProvince());
        account.setPostalCode(source.getPostalCode());
        account.setCountry(source.getCountry());
        account.setActive(source.isActive());

        return billingAccountRepository.save(account);
    }

    @Override
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    @Override
    public Invoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
    }

    @Override
    @Transactional
    public Invoice createInvoice(
            Long tenantId,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            LocalDate issueDate,
            LocalDate dueDate,
            String notes) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        TenantSubscription subscription = tenantSubscriptionRepository.findByTenant(tenant)
                .orElse(null);

        Invoice invoice = new Invoice();

        invoice.setTenant(tenant);
        invoice.setSubscription(subscription);
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssueDate(issueDate != null ? issueDate : LocalDate.now());
        invoice.setDueDate(dueDate != null ? dueDate : LocalDate.now().plusDays(7));
        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);

        BigDecimal total = safe(subtotal).add(safe(taxAmount));
        invoice.setTotalAmount(total);
        invoice.setAmountPaid(BigDecimal.ZERO);
        invoice.setNotes(notes);

        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public Payment recordPayment(
            Long invoiceId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String reference,
            String notes) {

        Invoice invoice = getInvoiceById(invoiceId);

        Payment payment = new Payment();

        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus(PaymentStatus.SUCCESSFUL);
        payment.setPaymentProvider(PaymentProvider.MANUAL);
        payment.setProviderStatus("MANUAL_RECORDED");
        payment.setPaidAt(LocalDateTime.now());
        payment.setReference(reference);
        payment.setNotes(notes);

        Payment savedPayment = paymentRepository.save(payment);

        BigDecimal newAmountPaid = invoice.getAmountPaid().add(safe(amount));
        invoice.setAmountPaid(newAmountPaid);

        if (newAmountPaid.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else if (newAmountPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        }

        invoiceRepository.save(invoice);

        return savedPayment;
    }

    private String generateInvoiceNumber() {

        String invoiceNumber;

        do {
            invoiceNumber = "INV-" + System.currentTimeMillis();
        } while (invoiceRepository.existsByInvoiceNumber(invoiceNumber));

        return invoiceNumber;
    }

    private BigDecimal safe(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
    
    @Override
    public List<Payment> getPaymentsForInvoice(Long invoiceId) {

        Invoice invoice = getInvoiceById(invoiceId);

        return paymentRepository.findByInvoiceOrderByPaidAtDesc(invoice);
    }
    
    @Override
    @Transactional
    public Invoice generateInvoiceFromSubscription(
            Long tenantId,
            LocalDate issueDate,
            LocalDate dueDate,
            String notes) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        TenantSubscription subscription = tenantSubscriptionRepository.findByTenant(tenant)
                .orElseThrow(() -> new RuntimeException("Tenant has no active subscription"));

        if (subscription.getPlan() == null) {
            throw new RuntimeException("Tenant subscription has no plan assigned");
        }

        BigDecimal subtotal;

        switch (subscription.getBillingCycle()) {
            case ANNUAL:
                subtotal = subscription.getPlan().getAnnualPrice();
                break;
            case MONTHLY:
            default:
                subtotal = subscription.getPlan().getMonthlyPrice();
                break;
        }

        BigDecimal taxAmount = subtotal.multiply(new BigDecimal("0.15"));

        Invoice invoice = new Invoice();

        invoice.setTenant(tenant);
        invoice.setSubscription(subscription);
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssueDate(issueDate != null ? issueDate : LocalDate.now());
        invoice.setDueDate(dueDate != null ? dueDate : LocalDate.now().plusDays(7));
        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(subtotal.add(taxAmount));
        invoice.setAmountPaid(BigDecimal.ZERO);
        invoice.setNotes(notes != null ? notes : "Generated from subscription plan");

        return invoiceRepository.save(invoice);
    }
    
    @Override
    @Transactional
    public int refreshOverdueInvoices() {

        List<Invoice> overdueInvoices = invoiceRepository.findByStatusInAndDueDateBefore(
                List.of(
                        InvoiceStatus.ISSUED,
                        InvoiceStatus.PARTIALLY_PAID
                ),
                LocalDate.now()
        );

        for (Invoice invoice : overdueInvoices) {
            if (invoice.getBalanceDue().compareTo(BigDecimal.ZERO) > 0) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
                
                if (invoice.getTenant() != null) {
                    String tenantName = invoice.getTenant().getName();
                    String invoiceNumber = invoice.getInvoiceNumber();
                    double amount = invoice.getTotalAmount().doubleValue();
                    String dueDate = invoice.getDueDate().toString();
                    notificationService.sendInvoiceOverdueAlert(tenantName, invoiceNumber, amount, dueDate);
                }
            }
        }

        invoiceRepository.saveAll(overdueInvoices);

        return overdueInvoices.size();
    }
    
    @Override
    public boolean tenantHasOverdueInvoices(Long tenantId) {
        return invoiceRepository.existsByTenantIdAndStatus(
                tenantId,
                InvoiceStatus.OVERDUE
        );
    }
    
    @Override
    @Transactional
    public int suspendOverdueSubscriptions() {

        List<Invoice> overdueInvoices = invoiceRepository.findByStatus(InvoiceStatus.OVERDUE);

        int suspendedCount = 0;

        for (Invoice invoice : overdueInvoices) {

            if (invoice.getTenant() == null) {
                continue;
            }

            TenantSubscription subscription = tenantSubscriptionRepository
                    .findByTenantId(invoice.getTenant().getId())
                    .orElse(null);

            if (subscription == null) {
                continue;
            }

            if (subscription.getStatus() == SubscriptionStatus.SUSPENDED) {
                continue;
            }

            subscription.setStatus(SubscriptionStatus.SUSPENDED);
            tenantSubscriptionRepository.save(subscription);

            suspendedCount++;
        }

        return suspendedCount;
    }
    
    @Override
    public BillingDashboardView getBillingDashboard() {

        List<Invoice> invoices = invoiceRepository.findAll();

        BillingDashboardView dashboard = new BillingDashboardView();

        dashboard.setTotalInvoices(invoices.size());

        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        BigDecimal overdue = BigDecimal.ZERO;

        long paidCount = 0;
        long overdueCount = 0;
        long issuedCount = 0;
        long partiallyPaidCount = 0;

        for (Invoice invoice : invoices) {

            totalInvoiced = totalInvoiced.add(invoice.getTotalAmount());
            totalPaid = totalPaid.add(invoice.getAmountPaid());

            BigDecimal balance = invoice.getBalanceDue();

            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                outstanding = outstanding.add(balance);
            }

            switch (invoice.getStatus()) {
                case PAID:
                    paidCount++;
                    break;
                case OVERDUE:
                    overdueCount++;
                    overdue = overdue.add(balance);
                    break;
                case ISSUED:
                    issuedCount++;
                    break;
                case PARTIALLY_PAID:
                    partiallyPaidCount++;
                    break;
                default:
                    break;
            }
        }

        dashboard.setTotalInvoiced(totalInvoiced);
        dashboard.setTotalPaid(totalPaid);
        dashboard.setOutstandingBalance(outstanding);
        dashboard.setOverdueBalance(overdue);
        dashboard.setPaidInvoices(paidCount);
        dashboard.setOverdueInvoices(overdueCount);
        dashboard.setIssuedInvoices(issuedCount);
        dashboard.setPartiallyPaidInvoices(partiallyPaidCount);

        return dashboard;
    }
}