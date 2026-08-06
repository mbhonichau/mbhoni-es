package com.mbhoni_creative.admincontroller;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
import com.mbhoni_creative.admindto.TenantDto;
import com.mbhoni_creative.admindto.TenantSubscriptionView;
import com.mbhoni_creative.adminentity.IndustryProfile;
import com.mbhoni_creative.adminentity.TenantSubscription;
import com.mbhoni_creative.adminrepository.IndustryProfileRepository;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.adminrepository.UserRepository;
import com.mbhoni_creative.adminservice.IndustryTemplateService;
import com.mbhoni_creative.adminservice.TenantService;

@Controller
@RequestMapping("/tenants")
public class TenantController {

    private final TenantService tenantService;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final UserRepository userRepository;
    private final IndustryProfileRepository industryProfileRepository;
    private final IndustryTemplateService industryTemplateService;

    public TenantController(
            TenantService tenantService,
            TenantSubscriptionRepository tenantSubscriptionRepository,
            UserRepository userRepository,
            IndustryProfileRepository industryProfileRepository,
            IndustryTemplateService industryTemplateService) {

        this.tenantService = tenantService;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.userRepository = userRepository;
        this.industryProfileRepository = industryProfileRepository;
        this.industryTemplateService = industryTemplateService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TENANT_VIEW')")
    public String listTenants(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String subscriptionStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {

        List<TenantSubscriptionView> tenantViews = tenantService.getAllTenants()
                .stream()
                .map(tenant -> {
                    TenantSubscriptionView view = new TenantSubscriptionView();

                    view.setTenantId(tenant.getId());
                    view.setTenantName(tenant.getName());
                    view.setTenantActive(tenant.isActive());
                    view.setIndustryProfileName(tenant.getIndustryProfileName());

                    long currentUsers = userRepository.countByTenantId(tenant.getId());
                    view.setCurrentUsers(currentUsers);

                    TenantSubscription subscription = tenantSubscriptionRepository
                            .findByTenantId(tenant.getId())
                            .orElse(null);

                    if (subscription != null && subscription.getPlan() != null) {
                        view.setPlanName(subscription.getPlan().getName());
                        view.setPlanCode(subscription.getPlan().getCode());
                        view.setSubscriptionStatus(subscription.getStatus());
                        view.setMaxUsers(subscription.getPlan().getMaxUsers());
                    }

                    return view;
                })
                .filter(tenant -> matchesTenantSearch(tenant, q))
                .filter(tenant -> matchesTenantStatus(tenant, status))
                .filter(tenant -> matchesSubscriptionStatus(tenant, subscriptionStatus))
                .collect(Collectors.toList());

        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        int fromIndex = Math.min(safePage * safeSize, tenantViews.size());
        int toIndex = Math.min(fromIndex + safeSize, tenantViews.size());

        model.addAttribute("tenants", tenantViews.subList(fromIndex, toIndex));
        model.addAttribute("page", new PageMeta(
                safePage,
                safeSize,
                tenantViews.size(),
                (int) Math.ceil((double) tenantViews.size() / safeSize)
        ));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("subscriptionStatus", subscriptionStatus);

        return "tenants/list";
    }
    
    @PostMapping("/apply-template")
    @PreAuthorize("hasAuthority('TENANT_EDIT')")
    public String applyIndustryTemplate(
            @RequestParam Long id,
            RedirectAttributes redirectAttributes) {

        industryTemplateService.applyTemplateToTenant(id);

        redirectAttributes.addFlashAttribute("successMessage", "Industry template applied successfully.");

        return "redirect:/tenants";
    }

    // =====================================================
    // CREATE TENANT (FORM)
    // =====================================================

    @GetMapping("/create")
    @PreAuthorize("hasAuthority('TENANT_CREATE')")
    public String createTenantForm(Model model) {

        model.addAttribute(
                "tenant",
                new TenantDto()
        );
        model.addAttribute(
        		"industryProfiles", industryProfileRepository.findByActiveTrueOrderByNameAsc());

        return "tenants/create";
    }

    // =====================================================
    // CREATE TENANT (SUBMIT)
    // =====================================================

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('TENANT_CREATE')")
    public String createTenant(
            @ModelAttribute TenantDto tenantDto,
            RedirectAttributes redirectAttributes) {

        tenantService.saveTenant(tenantDto);
        redirectAttributes.addFlashAttribute("successMessage", "Tenant created successfully.");

        return "redirect:/tenants";
    }

	 // =====================================================
	 // EDIT TENANT (FORM)
	 // =====================================================
	
	 @PostMapping("/edit")
	 @PreAuthorize("hasAuthority('TENANT_EDIT')")
	 public String editTenant(
	         @RequestParam Long id,
	         Model model) {
	
	     model.addAttribute("tenant", tenantService.getTenantById(id));
	     model.addAttribute("industryProfiles", industryProfileRepository.findByActiveTrueOrderByNameAsc());
	
	     return "tenants/edit";
	 }

	// =====================================================
	// UPDATE TENANT
	// =====================================================

	 @PostMapping("/update")
	 @PreAuthorize("hasAuthority('TENANT_EDIT')")
	 public String updateTenant(
	         @RequestParam Long id,
	         @ModelAttribute TenantDto tenantDto,
	         RedirectAttributes redirectAttributes) {

	     tenantService.updateTenant(id, tenantDto);
	     redirectAttributes.addFlashAttribute("successMessage", "Tenant updated successfully.");

	     return "redirect:/tenants";
	 }

	// =====================================================
	// DELETE TENANT
	// =====================================================

	 @PostMapping("/delete")
	 @PreAuthorize("hasAuthority('TENANT_DELETE')")
	 public String deleteTenant(
	         @RequestParam Long id,
	         RedirectAttributes redirectAttributes) {

	     tenantService.deleteTenant(id);
	     redirectAttributes.addFlashAttribute("successMessage", "Tenant deleted successfully.");

	     return "redirect:/tenants";
	 }

    private boolean matchesTenantSearch(TenantSubscriptionView tenant, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }

        String search = q.trim().toLowerCase(Locale.ROOT);
        return contains(tenant.getTenantName(), search)
                || contains(tenant.getIndustryProfileName(), search)
                || contains(tenant.getPlanName(), search)
                || contains(tenant.getPlanCode(), search);
    }

    private boolean matchesTenantStatus(TenantSubscriptionView tenant, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }

        if ("ACTIVE".equalsIgnoreCase(status)) {
            return tenant.isTenantActive();
        }

        if ("INACTIVE".equalsIgnoreCase(status)) {
            return !tenant.isTenantActive();
        }

        return true;
    }

    private boolean matchesSubscriptionStatus(TenantSubscriptionView tenant, String subscriptionStatus) {
        if (subscriptionStatus == null || subscriptionStatus.isBlank()) {
            return true;
        }

        return tenant.getSubscriptionStatus() != null
                && subscriptionStatus.equalsIgnoreCase(tenant.getSubscriptionStatus().name());
    }

    @PostMapping("/industries/create")
    @PreAuthorize("hasAuthority('TENANT_EDIT')")
    public String createIndustryProfile(
            @RequestParam String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "/tenants/create") String redirectUrl,
            RedirectAttributes redirectAttributes) {

        if (name == null || name.isBlank()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Industry name is required.");
            return "redirect:" + redirectUrl;
        }

        String normalizedCode = (code != null && !code.isBlank())
                ? code.trim().toUpperCase()
                : name.trim().toUpperCase().replaceAll("[^A-Z0-9]", "_");

        if (industryProfileRepository.findByCode(normalizedCode).isPresent()) {
            redirectAttributes.addFlashAttribute("errorMessage", "An industry profile with code '" + normalizedCode + "' already exists.");
            return "redirect:" + redirectUrl;
        }

        IndustryProfile profile = new IndustryProfile();
        profile.setCode(normalizedCode);
        profile.setName(name.trim());
        profile.setDescription(description);
        profile.setActive(true);

        industryProfileRepository.save(profile);
        redirectAttributes.addFlashAttribute("successMessage", "Industry Profile '" + profile.getName() + "' created successfully.");

        return "redirect:" + redirectUrl;
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }
}
