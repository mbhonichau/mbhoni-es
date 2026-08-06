package com.mbhoni_creative.adminservice.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.TenantInvoiceRepository;
import com.mbhoni_creative.adminrepository.TenantQuotationRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.TenantInvoicingService;

@Service
@Transactional
public class TenantInvoicingServiceImpl implements TenantInvoicingService {

    private final TenantInvoiceRepository invoiceRepository;
    private final TenantQuotationRepository quotationRepository;
    private final TenantRepository tenantRepository;

    public TenantInvoicingServiceImpl(
            TenantInvoiceRepository invoiceRepository,
            TenantQuotationRepository quotationRepository,
            TenantRepository tenantRepository) {
        this.invoiceRepository = invoiceRepository;
        this.quotationRepository = quotationRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantInvoice> getInvoicesForTenant(Long tenantId) {
        return invoiceRepository.findByTenantIdOrderByIdDesc(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantInvoice getInvoiceById(Long id) {
        return invoiceRepository.findById(id).orElse(null);
    }

    @Override
    public TenantInvoice saveInvoice(Long tenantId, TenantInvoice invoice, List<String> descriptions, List<BigDecimal> quantities, List<BigDecimal> unitPrices) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new IllegalArgumentException("Invalid Tenant ID"));
        invoice.setTenant(tenant);

        if (invoice.getInvoiceNumber() == null || invoice.getInvoiceNumber().isBlank()) {
            long count = invoiceRepository.countByTenantId(tenantId) + 1;
            invoice.setInvoiceNumber("INV-" + LocalDate.now().getYear() + "-" + String.format("%03d", count));
        }

        invoice.getItems().clear();
        if (descriptions != null) {
            for (int i = 0; i < descriptions.size(); i++) {
                String desc = descriptions.get(i);
                if (desc != null && !desc.isBlank()) {
                    BigDecimal qty = (quantities != null && i < quantities.size() && quantities.get(i) != null) ? quantities.get(i) : BigDecimal.ONE;
                    BigDecimal price = (unitPrices != null && i < unitPrices.size() && unitPrices.get(i) != null) ? unitPrices.get(i) : BigDecimal.ZERO;

                    TenantInvoiceItem item = new TenantInvoiceItem();
                    item.setDescription(desc);
                    item.setQuantity(qty);
                    item.setUnitPrice(price);
                    invoice.addItem(item);
                }
            }
        }

        invoice.recalculateTotals();
        return invoiceRepository.save(invoice);
    }

    @Override
    public void recordPayment(Long invoiceId, BigDecimal amountPaid) {
        TenantInvoice invoice = getInvoiceById(invoiceId);
        if (invoice != null && amountPaid != null && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentPaid = invoice.getAmountPaid() != null ? invoice.getAmountPaid() : BigDecimal.ZERO;
            BigDecimal newPaid = currentPaid.add(amountPaid);
            invoice.setAmountPaid(newPaid);

            if (newPaid.compareTo(invoice.getTotalAmount()) >= 0) {
                invoice.setStatus(InvoiceStatus.PAID);
            }
            invoiceRepository.save(invoice);
        }
    }

    @Override
    public void deleteInvoice(Long id) {
        invoiceRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TenantQuotation> getQuotationsForTenant(Long tenantId) {
        return quotationRepository.findByTenantIdOrderByIdDesc(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantQuotation getQuotationById(Long id) {
        return quotationRepository.findById(id).orElse(null);
    }

    @Override
    public TenantQuotation saveQuotation(Long tenantId, TenantQuotation quotation, List<String> descriptions, List<BigDecimal> quantities, List<BigDecimal> unitPrices) {
        Tenant tenant = tenantRepository.findById(tenantId).orElseThrow(() -> new IllegalArgumentException("Invalid Tenant ID"));
        quotation.setTenant(tenant);

        if (quotation.getQuotationNumber() == null || quotation.getQuotationNumber().isBlank()) {
            long count = quotationRepository.countByTenantId(tenantId) + 1;
            quotation.setQuotationNumber("QT-" + LocalDate.now().getYear() + "-" + String.format("%03d", count));
        }

        quotation.getItems().clear();
        if (descriptions != null) {
            for (int i = 0; i < descriptions.size(); i++) {
                String desc = descriptions.get(i);
                if (desc != null && !desc.isBlank()) {
                    BigDecimal qty = (quantities != null && i < quantities.size() && quantities.get(i) != null) ? quantities.get(i) : BigDecimal.ONE;
                    BigDecimal price = (unitPrices != null && i < unitPrices.size() && unitPrices.get(i) != null) ? unitPrices.get(i) : BigDecimal.ZERO;

                    TenantQuotationItem item = new TenantQuotationItem();
                    item.setDescription(desc);
                    item.setQuantity(qty);
                    item.setUnitPrice(price);
                    quotation.addItem(item);
                }
            }
        }

        quotation.recalculateTotals();
        return quotationRepository.save(quotation);
    }

    @Override
    public TenantInvoice convertQuotationToInvoice(Long quotationId) {
        TenantQuotation quotation = getQuotationById(quotationId);
        if (quotation == null) return null;

        TenantInvoice invoice = new TenantInvoice();
        invoice.setTenant(quotation.getTenant());
        long count = invoiceRepository.countByTenantId(quotation.getTenant().getId()) + 1;
        invoice.setInvoiceNumber("INV-" + LocalDate.now().getYear() + "-" + String.format("%03d", count));
        invoice.setCustomerName(quotation.getCustomerName());
        invoice.setCustomerEmail(quotation.getCustomerEmail());
        invoice.setCustomerPhone(quotation.getCustomerPhone());
        invoice.setCustomerAddress(quotation.getCustomerAddress());
        invoice.setNotes("Converted from Quotation #" + quotation.getQuotationNumber());
        invoice.setStatus(InvoiceStatus.DRAFT);

        for (TenantQuotationItem qItem : quotation.getItems()) {
            TenantInvoiceItem invItem = new TenantInvoiceItem();
            invItem.setDescription(qItem.getDescription());
            invItem.setQuantity(qItem.getQuantity());
            invItem.setUnitPrice(qItem.getUnitPrice());
            invItem.setTaxRate(qItem.getTaxRate());
            invoice.addItem(invItem);
        }

        invoice.recalculateTotals();
        TenantInvoice savedInvoice = invoiceRepository.save(invoice);

        quotation.setStatus(QuotationStatus.CONVERTED_TO_INVOICE);
        quotation.setConvertedInvoiceId(savedInvoice.getId());
        quotationRepository.save(quotation);

        return savedInvoice;
    }

    @Override
    public void deleteQuotation(Long id) {
        quotationRepository.deleteById(id);
    }
}
