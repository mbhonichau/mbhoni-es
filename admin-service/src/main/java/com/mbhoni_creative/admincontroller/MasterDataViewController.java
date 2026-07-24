package com.mbhoni_creative.admincontroller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.mbhoni_creative.adminentity.LookupCategory;
import com.mbhoni_creative.adminentity.LookupCode;
import com.mbhoni_creative.adminservice.MasterDataService;
import com.mbhoni_creative.config.TenantSecurityService;

@Controller
@RequestMapping("/master-data")
public class MasterDataViewController {

    private final MasterDataService masterDataService;
    private final TenantSecurityService tenantSecurityService;

    public MasterDataViewController(
            MasterDataService masterDataService,
            TenantSecurityService tenantSecurityService) {
        this.masterDataService = masterDataService;
        this.tenantSecurityService = tenantSecurityService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MASTER_DATA_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW')")
    public String index(@RequestParam(required = false) Long categoryId, Model model) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        var categories = masterDataService.getAllCategories(tenantId);
        model.addAttribute("categories", categories);

        if (categoryId != null) {
            model.addAttribute("selectedCategoryId", categoryId);
            model.addAttribute("codes", masterDataService.getLookupCodesByCategoryId(categoryId));
        } else if (!categories.isEmpty()) {
            model.addAttribute("selectedCategoryId", categories.get(0).getId());
            model.addAttribute("codes", masterDataService.getLookupCodesByCategoryId(categories.get(0).getId()));
        }

        model.addAttribute("newCategory", new LookupCategory());
        model.addAttribute("newCode", new LookupCode());
        return "master-data/list";
    }

    @PostMapping("/categories/save")
    @PreAuthorize("hasAuthority('MASTER_DATA_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW')")
    public String saveCategory(@ModelAttribute LookupCategory category) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        masterDataService.createCategory(category, tenantId);
        return "redirect:/master-data";
    }

    @PostMapping("/codes/save")
    @PreAuthorize("hasAuthority('MASTER_DATA_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW')")
    public String saveCode(@RequestParam Long categoryId, @ModelAttribute LookupCode code) {
        masterDataService.saveLookupCode(categoryId, code);
        return "redirect:/master-data?categoryId=" + categoryId;
    }
}
