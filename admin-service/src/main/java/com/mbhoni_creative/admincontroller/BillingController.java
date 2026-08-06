package com.mbhoni_creative.admincontroller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbhoni_creative.admindto.PageMeta;
import com.mbhoni_creative.admindto.TenantDto;
import com.mbhoni_creative.adminentity.BillingAccount;
import com.mbhoni_creative.adminentity.Invoice;
import com.mbhoni_creative.adminentity.InvoiceStatus;
import com.mbhoni_creative.adminentity.PaymentMethod;
import com.mbhoni_creative.adminentity.Expense;
import com.mbhoni_creative.adminentity.TenantCustomization;
import com.mbhoni_creative.adminrepository.BillingAccountRepository;
import com.mbhoni_creative.adminrepository.ExpenseRepository;
import com.mbhoni_creative.adminservice.BillingService;
import com.mbhoni_creative.adminservice.TenantService;
import com.mbhoni_creative.adminservice.TenantCustomizationService;
import com.mbhoni_creative.adminservice.InvoiceDocumentService;
import com.mbhoni_creative.config.TenantSecurityService;

@Controller
@RequestMapping("/billing")
public class BillingController {

    private final BillingService billingService;
    private final TenantService tenantService;
    private final TenantSecurityService tenantSecurityService;
    private final ExpenseRepository expenseRepository;
    private final BillingAccountRepository billingAccountRepository;
    private final TenantCustomizationService customizationService;
    private final InvoiceDocumentService invoiceDocumentService;

    public BillingController(
            BillingService billingService,
            TenantService tenantService,
            TenantSecurityService tenantSecurityService,
            ExpenseRepository expenseRepository,
            BillingAccountRepository billingAccountRepository,
            TenantCustomizationService customizationService,
            InvoiceDocumentService invoiceDocumentService) {

        this.billingService = billingService;
        this.tenantService = tenantService;
        this.tenantSecurityService = tenantSecurityService;
        this.expenseRepository = expenseRepository;
        this.billingAccountRepository = billingAccountRepository;
        this.customizationService = customizationService;
        this.invoiceDocumentService = invoiceDocumentService;
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
        model.addAttribute("tenants", getAccessibleTenants());
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

        Long activeTenantFilter = tenantId;
        if (!tenantSecurityService.isGlobalAdmin()) {
            activeTenantFilter = tenantSecurityService.getCurrentTenantId();
        }

        final Long effectiveTenantId = activeTenantFilter;
        List<Invoice> invoices = billingService.getAllInvoices()
                .stream()
                .filter(invoice -> matchesInvoiceSearch(invoice, q))
                .filter(invoice -> status == null || invoice.getStatus() == status)
                .filter(invoice -> effectiveTenantId == null
                        || (invoice.getTenant() != null && effectiveTenantId.equals(invoice.getTenant().getId())))
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
        model.addAttribute("selectedTenantId", effectiveTenantId);
        model.addAttribute("invoiceStatuses", InvoiceStatus.values());
        model.addAttribute("tenants", getAccessibleTenants());
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
    
    @GetMapping("/invoices/{id}/print")
    @PreAuthorize("hasAuthority('BILLING_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public String printInvoiceGet(@PathVariable Long id, Model model) {
        Invoice invoice = billingService.getInvoiceById(id);
        model.addAttribute("invoice", invoice);
        model.addAttribute("payments", billingService.getPaymentsForInvoice(id));

        if (invoice != null && invoice.getTenant() != null) {
            model.addAttribute("billingAccount", billingAccountRepository.findByTenantId(invoice.getTenant().getId()).orElse(null));
        }

        return "billing/invoice-print";
    }

    @PostMapping("/invoices/print")
    @PreAuthorize("hasAuthority('BILLING_VIEW')")
    public String printInvoicePost(@RequestParam Long id, Model model) {
        return printInvoiceGet(id, model);
    }

    private List<TenantDto> getAccessibleTenants() {
        if (tenantSecurityService.isGlobalAdmin()) {
            return tenantService.getAllTenants();
        }
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        if (tenantId == null) {
            return List.of();
        }
        try {
            return List.of(tenantService.getTenantById(tenantId));
        } catch (Exception e) {
            return List.of();
        }
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

    // =====================================================
    // SMALL BUSINESS EXPENSES & LEDGER TRACKER
    // =====================================================

    @GetMapping("/expenses")
    @PreAuthorize("hasAuthority('BILLING_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public String expenses(Model model) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        List<Expense> expenses;

        if (tenantSecurityService.isGlobalAdmin() && tenantId == null) {
            expenses = expenseRepository.findAll();
        } else if (tenantId != null) {
            expenses = expenseRepository.findByTenantIdOrderByExpenseDateDesc(tenantId);
        } else {
            expenses = List.of();
        }

        BigDecimal totalExpenses = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Revenue from invoices
        List<Invoice> invoices = billingService.getAllInvoices();
        BigDecimal totalPaidRevenue = invoices.stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.PAID)
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netProfit = totalPaidRevenue.subtract(totalExpenses);

        model.addAttribute("expenses", expenses);
        model.addAttribute("totalExpenses", totalExpenses);
        model.addAttribute("totalPaidRevenue", totalPaidRevenue);
        model.addAttribute("netProfit", netProfit);
        model.addAttribute("newExpense", new Expense());
        model.addAttribute("categories", List.of("OFFICE_RENT", "UTILITIES", "SALARIES", "SOFTWARE_SERVICES", "SUPPLIES", "TRAVEL_TRANSPORT", "EQUIPMENT", "MARKETING"));

        return "billing/expenses";
    }

    @PostMapping("/expenses/create")
    @PreAuthorize("hasAuthority('BILLING_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public String createExpense(
            @ModelAttribute Expense expense,
            RedirectAttributes redirectAttributes) {

        Long tenantId = tenantSecurityService.getCurrentTenantId();
        if (tenantId != null && expense.getTenant() == null) {
            com.mbhoni_creative.adminentity.Tenant t = new com.mbhoni_creative.adminentity.Tenant();
            t.setId(tenantId);
            expense.setTenant(t);
        } else if (expense.getTenant() == null && !tenantService.getAllTenants().isEmpty()) {
            com.mbhoni_creative.adminentity.Tenant t = new com.mbhoni_creative.adminentity.Tenant();
            t.setId(tenantService.getAllTenants().get(0).getId());
            expense.setTenant(t);
        }

        if (expense.getExpenseDate() == null) {
            expense.setExpenseDate(LocalDate.now());
        }

        expenseRepository.save(expense);
        redirectAttributes.addFlashAttribute("successMessage", "Business expense recorded successfully.");
        return "redirect:/billing/expenses";
    }

    @PostMapping("/expenses/delete")
    @PreAuthorize("hasAuthority('BILLING_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public String deleteExpense(
            @RequestParam Long id,
            RedirectAttributes redirectAttributes) {

        expenseRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Expense entry deleted.");
        return "redirect:/billing/expenses";
    }
}
