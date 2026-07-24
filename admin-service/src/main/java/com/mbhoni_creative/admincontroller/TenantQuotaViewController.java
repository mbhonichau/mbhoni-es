package com.mbhoni_creative.admincontroller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantQuota;
import com.mbhoni_creative.adminrepository.TenantQuotaRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.TenantService;

@Controller
@RequestMapping("/tenants/quotas")
public class TenantQuotaViewController {

    private final TenantQuotaRepository tenantQuotaRepository;
    private final TenantRepository tenantRepository;
    private final TenantService tenantService;

    public TenantQuotaViewController(
            TenantQuotaRepository tenantQuotaRepository,
            TenantRepository tenantRepository,
            TenantService tenantService) {
        this.tenantQuotaRepository = tenantQuotaRepository;
        this.tenantRepository = tenantRepository;
        this.tenantService = tenantService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TENANT_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public String index(Model model) {
        var tenants = tenantService.getAllTenants();
        model.addAttribute("tenants", tenants);

        var quotas = tenantQuotaRepository.findAll();
        model.addAttribute("quotas", quotas);
        model.addAttribute("newQuota", new TenantQuota());

        return "tenants/quotas";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('TENANT_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public String saveQuota(@RequestParam Long tenantId, @ModelAttribute TenantQuota quota) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        TenantQuota existing = tenantQuotaRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    TenantQuota q = new TenantQuota();
                    q.setTenant(tenant);
                    return q;
                });

        existing.setMaxUsers(quota.getMaxUsers());
        existing.setMaxApiRequestsPerMinute(quota.getMaxApiRequestsPerMinute());
        existing.setMaxStorageGb(quota.getMaxStorageGb());
        existing.setCustomQuotasJson(quota.getCustomQuotasJson());

        tenantQuotaRepository.save(existing);
        return "redirect:/tenants/quotas";
    }
}
