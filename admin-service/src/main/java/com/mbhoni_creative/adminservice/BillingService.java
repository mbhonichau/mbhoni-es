package com.mbhoni_creative.adminservice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.mbhoni_creative.admindto.BillingDashboardView;
import com.mbhoni_creative.adminentity.BillingAccount;
import com.mbhoni_creative.adminentity.Invoice;
import com.mbhoni_creative.adminentity.Payment;
import com.mbhoni_creative.adminentity.PaymentMethod;


public interface BillingService {

    List<BillingAccount> getAllBillingAccounts();

    BillingAccount getBillingAccountById(Long id);

    BillingAccount saveOrUpdateBillingAccount(Long tenantId, BillingAccount account);

    List<Invoice> getAllInvoices();

    Invoice getInvoiceById(Long id);
    
    List<Payment> getPaymentsForInvoice(Long invoiceId);
    
    int refreshOverdueInvoices();
    
    int suspendOverdueSubscriptions();

    boolean tenantHasOverdueInvoices(Long tenantId);
    
    BillingDashboardView getBillingDashboard();

    Invoice createInvoice(
            Long tenantId,
            BigDecimal subtotal,
            BigDecimal taxAmount,
            LocalDate issueDate,
            LocalDate dueDate,
            String notes
    );

    Payment recordPayment(
            Long invoiceId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            String reference,
            String notes
    );
    
    Invoice generateInvoiceFromSubscription(
            Long tenantId,
            LocalDate issueDate,
            LocalDate dueDate,
            String notes
    );
    
    
}