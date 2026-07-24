package com.mbhoni_creative.admincontroller;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbhoni_creative.admindto.PageMeta;
import com.mbhoni_creative.adminentity.BillingCycle;
import com.mbhoni_creative.adminentity.SubscriptionPlan;
import com.mbhoni_creative.adminentity.SubscriptionStatus;
import com.mbhoni_creative.adminentity.TenantSubscription;
import com.mbhoni_creative.adminservice.PlanModuleService;
import com.mbhoni_creative.adminservice.SubscriptionService;
import com.mbhoni_creative.adminservice.TenantService;

@Controller
@RequestMapping("/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final TenantService tenantService;
    private final PlanModuleService planModuleService;

    public SubscriptionController(
            SubscriptionService subscriptionService,
            TenantService tenantService,
            PlanModuleService planModuleService) {

        this.subscriptionService = subscriptionService;
        this.tenantService = tenantService;
        this.planModuleService = planModuleService;
    }

    @GetMapping("/plans")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_VIEW')")
    public String listPlans(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {

        List<SubscriptionPlan> plans = subscriptionService.getAllPlans()
                .stream()
                .filter(plan -> matchesPlanSearch(plan, q))
                .filter(plan -> matchesPlanStatus(plan, status))
                .toList();

        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        int fromIndex = Math.min(safePage * safeSize, plans.size());
        int toIndex = Math.min(fromIndex + safeSize, plans.size());

        model.addAttribute("plans", plans.subList(fromIndex, toIndex));
        model.addAttribute("page", new PageMeta(
                safePage,
                safeSize,
                plans.size(),
                (int) Math.ceil((double) plans.size() / safeSize)
        ));
        model.addAttribute("q", q);
        model.addAttribute("status", status);

        return "subscriptions/plans";
    }

    @GetMapping("/plans/create")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_CREATE')")
    public String createPlanForm(Model model) {

        model.addAttribute("plan", new SubscriptionPlan());

        return "subscriptions/plan-create";
    }

    @PostMapping("/plans/create")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_CREATE')")
    public String createPlan(
            @ModelAttribute SubscriptionPlan plan,
            RedirectAttributes redirectAttributes) {

        subscriptionService.savePlan(plan);
        redirectAttributes.addFlashAttribute("successMessage", "Subscription plan created successfully.");

        return "redirect:/subscriptions/plans";
    }

    @PostMapping("/plans/edit")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_EDIT')")
    public String editPlan(
            @RequestParam Long id,
            Model model) {

        model.addAttribute("plan", subscriptionService.getPlanById(id));

        return "subscriptions/plan-edit";
    }

    @PostMapping("/plans/update")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_EDIT')")
    public String updatePlan(
            @RequestParam Long id,
            @ModelAttribute SubscriptionPlan plan,
            RedirectAttributes redirectAttributes) {

        subscriptionService.updatePlan(id, plan);
        redirectAttributes.addFlashAttribute("successMessage", "Subscription plan updated successfully.");

        return "redirect:/subscriptions/plans";
    }

    @PostMapping("/plans/deactivate")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_DELETE')")
    public String deactivatePlan(
            @RequestParam Long id,
            RedirectAttributes redirectAttributes) {

        subscriptionService.deactivatePlan(id);
        redirectAttributes.addFlashAttribute("successMessage", "Subscription plan deactivated successfully.");

        return "redirect:/subscriptions/plans";
    }

    @GetMapping("/tenants")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_VIEW')")
    public String tenantSubscriptions(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SubscriptionStatus status,
            @RequestParam(required = false) BillingCycle billingCycle,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {

        List<TenantSubscription> subscriptions = subscriptionService.getAllTenantSubscriptions()
                .stream()
                .filter(subscription -> matchesSubscriptionSearch(subscription, q))
                .filter(subscription -> status == null || subscription.getStatus() == status)
                .filter(subscription -> billingCycle == null || subscription.getBillingCycle() == billingCycle)
                .toList();

        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        int fromIndex = Math.min(safePage * safeSize, subscriptions.size());
        int toIndex = Math.min(fromIndex + safeSize, subscriptions.size());

        model.addAttribute("subscriptions", subscriptions.subList(fromIndex, toIndex));
        model.addAttribute("page", new PageMeta(
                safePage,
                safeSize,
                subscriptions.size(),
                (int) Math.ceil((double) subscriptions.size() / safeSize)
        ));
        model.addAttribute("q", q);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedBillingCycle", billingCycle);
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("plans", subscriptionService.getActivePlans());
        model.addAttribute("statuses", SubscriptionStatus.values());
        model.addAttribute("billingCycles", BillingCycle.values());

        return "subscriptions/tenants";
    }

    @PostMapping("/tenants/assign")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_EDIT')")
    public String assignTenantSubscription(
            @RequestParam Long tenantId,
            @RequestParam Long planId,
            @RequestParam SubscriptionStatus status,
            @RequestParam BillingCycle billingCycle,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) LocalDate trialEndsAt,
            @RequestParam(defaultValue = "false") boolean autoRenew,
            RedirectAttributes redirectAttributes) {

        subscriptionService.assignTenantSubscription(
                tenantId,
                planId,
                status,
                billingCycle,
                startDate,
                endDate,
                trialEndsAt,
                autoRenew
        );

        redirectAttributes.addFlashAttribute("successMessage", "Tenant subscription updated successfully.");

        return "redirect:/subscriptions/tenants";
    }
    
    @PostMapping("/plans/modules")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_EDIT')")
    public String editPlanModules(
            @RequestParam Long id,
            Model model) {

        model.addAttribute("plan", subscriptionService.getPlanById(id));
        model.addAttribute("modules", planModuleService.getPlanModuleViews(id));

        return "subscriptions/plan-modules";
    }

    @PostMapping("/plans/modules/update")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_EDIT')")
    public String updatePlanModules(
            @RequestParam Long id,
            @RequestParam(required = false) Set<Long> allowedModuleIds,
            RedirectAttributes redirectAttributes) {

        planModuleService.updatePlanModules(id, allowedModuleIds);

        redirectAttributes.addFlashAttribute("successMessage", "Plan modules updated successfully.");

        return "redirect:/subscriptions/plans";
    }

    private boolean matchesPlanSearch(SubscriptionPlan plan, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }

        String search = q.trim().toLowerCase(Locale.ROOT);
        return contains(plan.getName(), search)
                || contains(plan.getCode(), search)
                || contains(plan.getDescription(), search);
    }

    private boolean matchesPlanStatus(SubscriptionPlan plan, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }

        if ("ACTIVE".equalsIgnoreCase(status)) {
            return plan.isActive();
        }

        if ("INACTIVE".equalsIgnoreCase(status)) {
            return !plan.isActive();
        }

        return true;
    }

    private boolean matchesSubscriptionSearch(TenantSubscription subscription, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }

        String search = q.trim().toLowerCase(Locale.ROOT);
        return (subscription.getTenant() != null && contains(subscription.getTenant().getName(), search))
                || (subscription.getPlan() != null && contains(subscription.getPlan().getName(), search))
                || (subscription.getPlan() != null && contains(subscription.getPlan().getCode(), search));
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }
}
