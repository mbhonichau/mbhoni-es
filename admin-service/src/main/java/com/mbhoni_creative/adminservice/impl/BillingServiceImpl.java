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
import com.mbhoni_creative.config.TenantSecurityService;

@Service
public class BillingServiceImpl implements BillingService {

    private final BillingAccountRepository billingAccountRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final TenantRepository tenantRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final NotificationService notificationService;
    private final TenantSecurityService tenantSecurityService;
    private final com.mbhoni_creative.billing.SubscriptionLifecycle lifecycle;
    private final com.mbhoni_creative.config.TenantGovernance governance;

    public BillingServiceImpl(
            BillingAccountRepository billingAccountRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            TenantRepository tenantRepository,
            TenantSubscriptionRepository tenantSubscriptionRepository,
            NotificationService notificationService,
            TenantSecurityService tenantSecurityService,
            com.mbhoni_creative.billing.SubscriptionLifecycle lifecycle, com.mbhoni_creative.config.TenantGovernance governance) {

        this.billingAccountRepository = billingAccountRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.tenantRepository = tenantRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.notificationService = notificationService;
        this.tenantSecurityService = tenantSecurityService;
        this.lifecycle = lifecycle;
        this.governance = governance;
    }

    @Override
    public List<BillingAccount> getAllBillingAccounts() {
        governance.requireBillingOwner(tenantSecurityService.getCurrentTenantId());
        if (tenantSecurityService.isGlobalAdmin()) {
            return billingAccountRepository.findAll();
        }
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        if (tenantId == null) {
            return List.of();
        }
        return billingAccountRepository.findByTenantId(tenantId)
                .map(List::of)
                .orElseGet(List::of);
    }

    @Override
    public BillingAccount getBillingAccountById(Long id) {
        BillingAccount account = billingAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Billing account not found"));

        enforceTenantAccess(account.getTenant());
        return account;
    }

    @Override
    @Transactional
    public BillingAccount saveOrUpdateBillingAccount(Long tenantId, BillingAccount source) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        enforceTenantAccess(tenant);

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
        governance.requireBillingOwner(tenantSecurityService.getCurrentTenantId());
        if (tenantSecurityService.isGlobalAdmin()) {
            return invoiceRepository.findAll();
        }
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        if (tenantId == null) {
            return List.of();
        }
        return invoiceRepository.findByTenantId(tenantId);
    }

    @Override
    public Invoice getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        enforceTenantAccess(invoice.getTenant());
        return invoice;
    }

    @Override
    public List<Payment> getPaymentsForInvoice(Long invoiceId) {

        Invoice invoice = getInvoiceById(invoiceId);

        return paymentRepository.findByInvoiceOrderByPaidAtDesc(invoice);
    }
    
    @Override
    @Transactional
    public int refreshOverdueInvoices() {
        requirePlatformAdmin();

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
        governance.requireBillingOwner(tenantId);
        return invoiceRepository.existsByTenantIdAndStatus(
                tenantId,
                InvoiceStatus.OVERDUE
        );
    }
    
    @Override
    @Transactional
    public int suspendOverdueSubscriptions() {
        requirePlatformAdmin();
        int suspendedCount = 0;
        for (Long tenantId : tenantSubscriptionRepository.findLifecycleCandidateTenantIds()) {
            TenantSubscription subscription = tenantSubscriptionRepository.lockByTenantId(tenantId).orElseThrow();
            SubscriptionStatus before = subscription.getStatus();
            lifecycle.reconcile(subscription);
            if (before != SubscriptionStatus.SUSPENDED && subscription.getStatus() == SubscriptionStatus.SUSPENDED) suspendedCount++;
        }

        return suspendedCount;
    }
    
    @Override
    public BillingDashboardView getBillingDashboard() {
        governance.requireBillingOwner(tenantSecurityService.getCurrentTenantId());

        List<Invoice> invoices;
        if (tenantSecurityService.isGlobalAdmin()) {
            invoices = invoiceRepository.findAll();
        } else {
            Long tenantId = tenantSecurityService.getCurrentTenantId();
            invoices = tenantId != null ? invoiceRepository.findByTenantId(tenantId) : List.of();
        }

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

    private void enforceTenantAccess(Tenant tenant) {
        governance.requireBillingOwner(tenant == null ? null : tenant.getId());
        if (tenantSecurityService.isGlobalAdmin()) {
            return;
        }
        Long currentTenantId = tenantSecurityService.getCurrentTenantId();
        if (currentTenantId == null || tenant == null || !tenant.getId().equals(currentTenantId)) {
            throw new RuntimeException("Access denied");
        }
    }

    private void requirePlatformAdmin() {
        if (!tenantSecurityService.isGlobalAdmin()) throw new org.springframework.security.access.AccessDeniedException("Platform administrator required");
    }
}

