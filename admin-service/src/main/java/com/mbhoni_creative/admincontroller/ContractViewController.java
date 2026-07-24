package com.mbhoni_creative.admincontroller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.mbhoni_creative.adminentity.Contract;
import com.mbhoni_creative.adminservice.ContractService;
import com.mbhoni_creative.adminservice.TenantService;
import com.mbhoni_creative.config.TenantSecurityService;

@Controller
@RequestMapping("/contracts")
public class ContractViewController {

    private final ContractService contractService;
    private final TenantService tenantService;
    private final TenantSecurityService tenantSecurityService;

    public ContractViewController(
            ContractService contractService,
            TenantService tenantService,
            TenantSecurityService tenantSecurityService) {
        this.contractService = contractService;
        this.tenantService = tenantService;
        this.tenantSecurityService = tenantSecurityService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CONTRACT_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW')")
    public String index(Model model) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        model.addAttribute("contracts", contractService.getContractsByTenant(tenantId));
        model.addAttribute("tenants", tenantService.getAllTenants());
        model.addAttribute("newContract", new Contract());
        return "contracts/list";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('CONTRACT_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW')")
    public String saveContract(@RequestParam(required = false) Long tenantId, @ModelAttribute Contract contract) {
        Long targetTenantId = tenantId != null ? tenantId : tenantSecurityService.getCurrentTenantId();
        contractService.createOrUpdateContract(targetTenantId, contract);
        return "redirect:/contracts";
    }
}
