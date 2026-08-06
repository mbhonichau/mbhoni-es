package com.mbhoni_creative.adminservice;

import java.math.BigDecimal;
import java.util.List;

import com.mbhoni_creative.adminentity.TenantInvoice;
import com.mbhoni_creative.adminentity.TenantQuotation;

public interface TenantInvoicingService {

    List<TenantInvoice> getInvoicesForTenant(Long tenantId);
    TenantInvoice getInvoiceById(Long id);
    TenantInvoice saveInvoice(Long tenantId, TenantInvoice invoice, List<String> descriptions, List<BigDecimal> quantities, List<BigDecimal> unitPrices);
    void recordPayment(Long invoiceId, BigDecimal amountPaid);
    void deleteInvoice(Long id);

    List<TenantQuotation> getQuotationsForTenant(Long tenantId);
    TenantQuotation getQuotationById(Long id);
    TenantQuotation saveQuotation(Long tenantId, TenantQuotation quotation, List<String> descriptions, List<BigDecimal> quantities, List<BigDecimal> unitPrices);
    TenantInvoice convertQuotationToInvoice(Long quotationId);
    void deleteQuotation(Long id);
}
