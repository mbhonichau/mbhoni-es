package com.mbhoni_creative.admincontroller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbhoni_creative.admindto.PageMeta;
import com.mbhoni_creative.adminentity.BillingAccount;
import com.mbhoni_creative.adminentity.Invoice;
import com.mbhoni_creative.adminentity.InvoiceStatus;
import com.mbhoni_creative.adminentity.PaymentMethod;
import com.mbhoni_creative.adminservice.BillingService;
import com.mbhoni_creative.adminservice.TenantService;

@Controller
@RequestMapping("/billing")
public class BillingController {

    private final BillingService billingService;
    private final TenantService tenantService;

    public BillingController(
            BillingService billingService,
            TenantService tenantService) {

        this.billingService = billingService;
        this.tenantService = tenantService;
    }

    @GetMapping("/accounts")
    @PreAuthorize("hasAuthority('BILLING_VIEW')")
    public String accounts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {

        List<BillingAccount> accounts = billingService.getAllBillingAccounts()
                .stream()
                .filter(account -> matchesAccountSearch(account, q))
                .filter(account -> matchesAccountStatus(account, status))
                .toList();

        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        int fromIndex = Math.min(safePage * safeSize, accounts.size());
        int toIndex = Math.min(fromIndex + safeSize, accounts.size());

        model.addAttribute("accounts", accounts.subList(fromIndex, toIndex));
        model.addAttribute("page", new PageMeta(
                safePage,
                safeSize,
                accounts.size(),
                (int) Math.ceil((double) accounts.size() / safeSize)
        ));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("account", new BillingAccount());

        return "billing/accounts";
    }

    @PostMapping("/accounts/save")
    @PreAuthorize("hasAuthority('BILLING_EDIT')")
    public String saveAccount(
            @RequestParam Long tenantId,
            @ModelAttribute BillingAccount account,
            RedirectAttributes redirectAttributes) {

        billingService.saveOrUpdateBillingAccount(tenantId, account);
        redirectAttributes.addFlashAttribute("successMessage", "Billing account saved successfully.");

        return "redirect:/billing/accounts";
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAuthority('BILLING_VIEW')")
    public String invoices(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {

        List<Invoice> invoices = billingService.getAllInvoices()
                .stream()
                .filter(invoice -> matchesInvoiceSearch(invoice, q))
                .filter(invoice -> status == null || invoice.getStatus() == status)
                .filter(invoice -> tenantId == null
                        || (invoice.getTenant() != null && tenantId.equals(invoice.getTenant().getId())))
                .toList();

        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        int fromIndex = Math.min(safePage * safeSize, invoices.size());
        int toIndex = Math.min(fromIndex + safeSize, invoices.size());

        model.addAttribute("invoices", invoices.subList(fromIndex, toIndex));
        model.addAttribute("page", new PageMeta(
                safePage,
                safeSize,
                invoices.size(),
                (int) Math.ceil((double) invoices.size() / safeSize)
        ));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("selectedTenantId", tenantId);
        model.addAttribute("invoiceStatuses", InvoiceStatus.values());
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("paymentMethods", PaymentMethod.values());

        return "billing/invoices";
    }

    @PostMapping("/invoices/create")
    @PreAuthorize("hasAuthority('BILLING_CREATE')")
    public String createInvoice(
            @RequestParam Long tenantId,
            @RequestParam BigDecimal subtotal,
            @RequestParam(defaultValue = "0") BigDecimal taxAmount,
            @RequestParam(required = false) LocalDate issueDate,
            @RequestParam(required = false) LocalDate dueDate,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {

        billingService.createInvoice(
                tenantId,
                subtotal,
                taxAmount,
                issueDate,
                dueDate,
                notes
        );

        redirectAttributes.addFlashAttribute("successMessage", "Invoice created successfully.");

        return "redirect:/billing/invoices";
    }

    @PostMapping("/payments/create")
    @PreAuthorize("hasAuthority('BILLING_EDIT')")
    public String recordPayment(
            @RequestParam Long invoiceId,
            @RequestParam BigDecimal amount,
            @RequestParam PaymentMethod paymentMethod,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {

        billingService.recordPayment(
                invoiceId,
                amount,
                paymentMethod,
                reference,
                notes
        );

        redirectAttributes.addFlashAttribute("successMessage", "Payment recorded successfully.");

        return "redirect:/billing/invoices";
    }
    
    @PostMapping("/invoices/view")
    @PreAuthorize("hasAuthority('BILLING_VIEW')")
    public String invoiceDetail(
            @RequestParam Long id,
            Model model) {

        model.addAttribute("invoice", billingService.getInvoiceById(id));
        model.addAttribute("payments", billingService.getPaymentsForInvoice(id));
        model.addAttribute("paymentMethods", PaymentMethod.values());

        return "billing/invoice-detail";
    }
    
    @PostMapping("/invoices/generate")
    @PreAuthorize("hasAuthority('BILLING_CREATE')")
    public String generateInvoiceFromSubscription(
            @RequestParam Long tenantId,
            @RequestParam(required = false) LocalDate issueDate,
            @RequestParam(required = false) LocalDate dueDate,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {

        billingService.generateInvoiceFromSubscription(
                tenantId,
                issueDate,
                dueDate,
                notes
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Subscription invoice generated successfully."
        );

        return "redirect:/billing/invoices";
    }
    
    @PostMapping("/invoices/refresh-overdue")
    @PreAuthorize("hasAuthority('BILLING_EDIT')")
    public String refreshOverdueInvoices(
            RedirectAttributes redirectAttributes) {

        int updatedCount = billingService.refreshOverdueInvoices();

        redirectAttributes.addFlashAttribute(
                "successMessage",
                updatedCount + " overdue invoice(s) refreshed."
        );

        return "redirect:/billing/invoices";
    }
    
    @PostMapping("/invoices/suspend-overdue")
    @PreAuthorize("hasAuthority('BILLING_EDIT')")
    public String suspendOverdueSubscriptions(
            RedirectAttributes redirectAttributes) {

        int suspendedCount = billingService.suspendOverdueSubscriptions();

        redirectAttributes.addFlashAttribute(
                "successMessage",
                suspendedCount + " subscription(s) suspended for overdue billing."
        );

        return "redirect:/billing/invoices";
    }
    
    @GetMapping
    @PreAuthorize("hasAuthority('BILLING_VIEW')")
    public String dashboard(Model model) {

        model.addAttribute("billing", billingService.getBillingDashboard());

        return "billing/dashboard";
    }
    
    @PostMapping("/invoices/print")
    @PreAuthorize("hasAuthority('BILLING_VIEW')")
    public String printInvoice(
            @RequestParam Long id,
            Model model) {

        model.addAttribute("invoice", billingService.getInvoiceById(id));
        model.addAttribute("payments", billingService.getPaymentsForInvoice(id));

        return "billing/invoice-print";
    }

    private boolean matchesAccountSearch(BillingAccount account, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }

        String search = q.trim().toLowerCase(Locale.ROOT);
        return contains(account.getBillingName(), search)
                || contains(account.getBillingEmail(), search)
                || contains(account.getTaxNumber(), search)
                || contains(account.getCity(), search)
                || contains(account.getCountry(), search)
                || (account.getTenant() != null && contains(account.getTenant().getName(), search));
    }

    private boolean matchesAccountStatus(BillingAccount account, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }

        if ("ACTIVE".equalsIgnoreCase(status)) {
            return account.isActive();
        }

        if ("INACTIVE".equalsIgnoreCase(status)) {
            return !account.isActive();
        }

        return true;
    }

    private boolean matchesInvoiceSearch(Invoice invoice, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }

        String search = q.trim().toLowerCase(Locale.ROOT);
        return contains(invoice.getInvoiceNumber(), search)
                || contains(invoice.getNotes(), search)
                || (invoice.getTenant() != null && contains(invoice.getTenant().getName(), search));
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }
}
