package com.mbhoni_creative.adminservice;

import com.mbhoni_creative.adminentity.Invoice;
import com.mbhoni_creative.adminentity.TenantInvoice;
import com.mbhoni_creative.adminentity.TenantQuotation;

public interface InvoiceDocumentService {

    byte[] generateSampleInvoiceExcelTemplate();
    byte[] generateSampleInvoiceWordTemplate();

    String renderInvoiceFromExcel(byte[] excelBytes, Invoice invoice);
    String renderInvoiceFromWord(byte[] wordBytes, Invoice invoice);

    String renderTenantInvoiceFromExcel(byte[] excelBytes, TenantInvoice invoice);
    String renderTenantInvoiceFromWord(byte[] wordBytes, TenantInvoice invoice);

    String renderTenantQuotationFromExcel(byte[] excelBytes, TenantQuotation quotation);
    String renderTenantQuotationFromWord(byte[] wordBytes, TenantQuotation quotation);
}
