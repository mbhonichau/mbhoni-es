package com.mbhoni_creative.admincontroller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.BillingAccountRepository;
import com.mbhoni_creative.adminservice.*;
import com.mbhoni_creative.config.TenantSecurityService;

@Controller
@RequestMapping("/tenant")
public class TenantInvoiceViewController {

    private final TenantInvoicingService invoicingService;
    private final TenantCustomizationService customizationService;
    private final InvoiceDocumentService invoiceDocumentService;
    private final TenantSecurityService tenantSecurityService;
    private final TenantService tenantService;
    private final BillingAccountRepository billingAccountRepository;

    public TenantInvoiceViewController(
            TenantInvoicingService invoicingService,
            TenantCustomizationService customizationService,
            InvoiceDocumentService invoiceDocumentService,
            TenantSecurityService tenantSecurityService,
            TenantService tenantService,
            BillingAccountRepository billingAccountRepository) {
        this.invoicingService = invoicingService;
        this.customizationService = customizationService;
        this.invoiceDocumentService = invoiceDocumentService;
        this.tenantSecurityService = tenantSecurityService;
        this.tenantService = tenantService;
        this.billingAccountRepository = billingAccountRepository;
    }

    // =====================================================
    // 1. TENANT INVOICES LIST & MANAGEMENT
    // =====================================================

    @GetMapping("/invoices")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_VIEW')")
    public String listInvoices(Model model) {
        Long tenantId = getEffectiveTenantId();
        List<TenantInvoice> invoices = invoicingService.getInvoicesForTenant(tenantId);

        BigDecimal totalRevenue = invoices.stream().map(TenantInvoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPaid = invoices.stream().map(TenantInvoice::getAmountPaid).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalUnpaid = invoices.stream().map(TenantInvoice::getBalanceDue).reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("invoices", invoices);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("totalUnpaid", totalUnpaid);
        model.addAttribute("statuses", InvoiceStatus.values());
        return "invoices/tenant-invoices-list";
    }

    @PostMapping("/invoices/save")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_EDIT')")
    public String saveInvoice(
            @ModelAttribute TenantInvoice invoice,
            @RequestParam(name = "itemDescription", required = false) List<String> itemDescriptions,
            @RequestParam(name = "itemQuantity", required = false) List<BigDecimal> itemQuantities,
            @RequestParam(name = "itemUnitPrice", required = false) List<BigDecimal> itemUnitPrices,
            RedirectAttributes redirectAttributes) {

        Long tenantId = getEffectiveTenantId();
        try {
            invoicingService.saveInvoice(tenantId, invoice, itemDescriptions, itemQuantities, itemUnitPrices);
            redirectAttributes.addFlashAttribute("successMessage", "Invoice #" + invoice.getInvoiceNumber() + " saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving invoice: " + e.getMessage());
        }
        return "redirect:/tenant/invoices";
    }

    @PostMapping("/invoices/{id}/pay")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_EDIT')")
    public String recordInvoicePayment(
            @PathVariable Long id,
            @RequestParam(name = "paymentAmount") BigDecimal paymentAmount,
            RedirectAttributes redirectAttributes) {

        try {
            invoicingService.recordPayment(id, paymentAmount);
            redirectAttributes.addFlashAttribute("successMessage", "Payment of R " + paymentAmount + " recorded successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error recording payment: " + e.getMessage());
        }
        return "redirect:/tenant/invoices";
    }

    @PostMapping("/invoices/{id}/delete")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public String deleteInvoice(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            invoicingService.deleteInvoice(id);
            redirectAttributes.addFlashAttribute("successMessage", "Invoice deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting invoice: " + e.getMessage());
        }
        return "redirect:/tenant/invoices";
    }

    @GetMapping("/invoices/{id}/print")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_VIEW')")
    public String printInvoice(@PathVariable Long id, Model model) {
        TenantInvoice invoice = invoicingService.getInvoiceById(id);
        model.addAttribute("invoice", invoice);

        if (invoice != null && invoice.getTenant() != null) {
            model.addAttribute("billingAccount", billingAccountRepository.findByTenantId(invoice.getTenant().getId()).orElse(null));

            TenantCustomization customization = customizationService.getOrCreateForTenant(invoice.getTenant().getId());
            String engineType = customization != null ? customization.getInvoiceEngineType() : "EXCEL";

            String customHtml;
            if ("WORD".equalsIgnoreCase(engineType)) {
                byte[] wordBytes = (customization != null && customization.getInvoiceWordTemplate() != null && customization.getInvoiceWordTemplate().length > 0)
                        ? customization.getInvoiceWordTemplate()
                        : invoiceDocumentService.generateSampleInvoiceWordTemplate();
                customHtml = invoiceDocumentService.renderTenantInvoiceFromWord(wordBytes, invoice);
            } else {
                byte[] excelBytes = (customization != null && customization.getInvoiceExcelTemplate() != null && customization.getInvoiceExcelTemplate().length > 0)
                        ? customization.getInvoiceExcelTemplate()
                        : invoiceDocumentService.generateSampleInvoiceExcelTemplate();
                customHtml = invoiceDocumentService.renderTenantInvoiceFromExcel(excelBytes, invoice);
            }

            model.addAttribute("customInvoiceHtml", customHtml);
        }

        return "invoices/invoice-print";
    }

    // =====================================================
    // 2. TENANT QUOTATIONS & ESTIMATES MANAGEMENT
    // =====================================================

    @GetMapping("/quotations")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_VIEW')")
    public String listQuotations(Model model) {
        Long tenantId = getEffectiveTenantId();
        List<TenantQuotation> quotations = invoicingService.getQuotationsForTenant(tenantId);

        BigDecimal totalQuoted = quotations.stream().map(TenantQuotation::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("quotations", quotations);
        model.addAttribute("totalQuoted", totalQuoted);
        model.addAttribute("statuses", QuotationStatus.values());
        return "invoices/tenant-quotations-list";
    }

    @PostMapping("/quotations/save")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_EDIT')")
    public String saveQuotation(
            @ModelAttribute TenantQuotation quotation,
            @RequestParam(name = "itemDescription", required = false) List<String> itemDescriptions,
            @RequestParam(name = "itemQuantity", required = false) List<BigDecimal> itemQuantities,
            @RequestParam(name = "itemUnitPrice", required = false) List<BigDecimal> itemUnitPrices,
            RedirectAttributes redirectAttributes) {

        Long tenantId = getEffectiveTenantId();
        try {
            invoicingService.saveQuotation(tenantId, quotation, itemDescriptions, itemQuantities, itemUnitPrices);
            redirectAttributes.addFlashAttribute("successMessage", "Quotation #" + quotation.getQuotationNumber() + " saved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving quotation: " + e.getMessage());
        }
        return "redirect:/tenant/quotations";
    }

    @PostMapping("/quotations/{id}/convert")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_EDIT')")
    public String convertQuotationToInvoice(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            TenantInvoice invoice = invoicingService.convertQuotationToInvoice(id);
            redirectAttributes.addFlashAttribute("successMessage", "Quotation successfully converted to Invoice #" + invoice.getInvoiceNumber() + "!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error converting quotation: " + e.getMessage());
        }
        return "redirect:/tenant/invoices";
    }

    @PostMapping("/quotations/{id}/delete")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public String deleteQuotation(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            invoicingService.deleteQuotation(id);
            redirectAttributes.addFlashAttribute("successMessage", "Quotation deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting quotation: " + e.getMessage());
        }
        return "redirect:/tenant/quotations";
    }

    @GetMapping("/quotations/{id}/print")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_VIEW')")
    public String printQuotation(@PathVariable Long id, Model model) {
        TenantQuotation quotation = invoicingService.getQuotationById(id);
        model.addAttribute("quotation", quotation);

        if (quotation != null && quotation.getTenant() != null) {
            model.addAttribute("billingAccount", billingAccountRepository.findByTenantId(quotation.getTenant().getId()).orElse(null));

            TenantCustomization customization = customizationService.getOrCreateForTenant(quotation.getTenant().getId());
            String engineType = customization != null ? customization.getInvoiceEngineType() : "EXCEL";

            String customHtml;
            if ("WORD".equalsIgnoreCase(engineType)) {
                byte[] wordBytes = (customization != null && customization.getInvoiceWordTemplate() != null && customization.getInvoiceWordTemplate().length > 0)
                        ? customization.getInvoiceWordTemplate()
                        : invoiceDocumentService.generateSampleInvoiceWordTemplate();
                customHtml = invoiceDocumentService.renderTenantQuotationFromWord(wordBytes, quotation);
            } else {
                byte[] excelBytes = (customization != null && customization.getInvoiceExcelTemplate() != null && customization.getInvoiceExcelTemplate().length > 0)
                        ? customization.getInvoiceExcelTemplate()
                        : invoiceDocumentService.generateSampleInvoiceExcelTemplate();
                customHtml = invoiceDocumentService.renderTenantQuotationFromExcel(excelBytes, quotation);
            }

            model.addAttribute("customInvoiceHtml", customHtml);
        }

        return "invoices/invoice-print";
    }

    // =====================================================
    // 3. DUAL EXCEL & WORD TEMPLATE STUDIO
    // =====================================================

    @GetMapping("/invoices/builder")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_VIEW')")
    public String invoiceBuilder(Model model) {
        Long tenantId = getEffectiveTenantId();

        TenantCustomization customization = tenantId != null ? customizationService.getOrCreateForTenant(tenantId) : null;
        String engineType = customization != null ? customization.getInvoiceEngineType() : "EXCEL";
        String uploadedExcelFileName = customization != null ? customization.getInvoiceExcelFileName() : null;
        String uploadedWordFileName = customization != null ? customization.getInvoiceWordFileName() : null;
        boolean hasCustomExcel = customization != null && customization.getInvoiceExcelTemplate() != null && customization.getInvoiceExcelTemplate().length > 0;
        boolean hasCustomWord = customization != null && customization.getInvoiceWordTemplate() != null && customization.getInvoiceWordTemplate().length > 0;

        model.addAttribute("tenantId", tenantId);
        model.addAttribute("engineType", engineType);
        model.addAttribute("uploadedExcelFileName", uploadedExcelFileName);
        model.addAttribute("uploadedWordFileName", uploadedWordFileName);
        model.addAttribute("hasCustomExcel", hasCustomExcel);
        model.addAttribute("hasCustomWord", hasCustomWord);
        return "invoices/invoice-builder";
    }

    @PostMapping("/invoices/builder/save")
    @PreAuthorize("@tenantEntitlementService.isModuleEnabled('SERVICE') or @tenantEntitlementService.isModuleEnabled('CONTRACT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('BILLING_EDIT')")
    public String saveInvoiceBuilder(
            @RequestParam(name = "engineType", defaultValue = "EXCEL") String engineType,
            @RequestParam(name = "excelFile", required = false) MultipartFile excelFile,
            @RequestParam(name = "wordFile", required = false) MultipartFile wordFile,
            RedirectAttributes redirectAttributes) {

        Long tenantId = getEffectiveTenantId();
        if (tenantId != null) {
            try {
                TenantCustomization customization = customizationService.getOrCreateForTenant(tenantId);
                customization.setInvoiceEngineType(engineType);

                if ("EXCEL".equalsIgnoreCase(engineType) && excelFile != null && !excelFile.isEmpty()) {
                    customization.setInvoiceExcelTemplate(excelFile.getBytes());
                    customization.setInvoiceExcelFileName(excelFile.getOriginalFilename());
                    redirectAttributes.addFlashAttribute("successMessage", "Custom Invoice Excel template (" + excelFile.getOriginalFilename() + ") uploaded & set as active engine!");
                } else if ("WORD".equalsIgnoreCase(engineType) && wordFile != null && !wordFile.isEmpty()) {
                    customization.setInvoiceWordTemplate(wordFile.getBytes());
                    customization.setInvoiceWordFileName(wordFile.getOriginalFilename());
                    redirectAttributes.addFlashAttribute("successMessage", "Custom Invoice Word template (" + wordFile.getOriginalFilename() + ") uploaded & set as active engine!");
                } else {
                    redirectAttributes.addFlashAttribute("successMessage", "Active tenant invoice engine switched to " + engineType + "!");
                }

                customizationService.saveForTenant(tenantId, customization);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Failed to process invoice template: " + e.getMessage());
            }
        }

        return "redirect:/tenant/invoices/builder";
    }

    @GetMapping("/invoices/excel/sample")
    public ResponseEntity<byte[]> downloadSampleInvoiceExcel() {
        byte[] bytes = invoiceDocumentService.generateSampleInvoiceExcelTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Tenant_Invoice_Sample_Template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @GetMapping("/invoices/word/sample")
    public ResponseEntity<byte[]> downloadSampleInvoiceWord() {
        byte[] bytes = invoiceDocumentService.generateSampleInvoiceWordTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Tenant_Invoice_Sample_Template.docx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(bytes);
    }

    private Long getEffectiveTenantId() {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        if (tenantId == null && !tenantService.getAllTenants().isEmpty()) {
            return tenantService.getAllTenants().get(0).getId();
        }
        return tenantId;
    }
}
