package com.mbhoni_creative.admincontroller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.mbhoni_creative.adminentity.ServiceCatalog;
import com.mbhoni_creative.adminentity.ServiceSla;
import com.mbhoni_creative.adminservice.ServiceCatalogService;

@Controller
@RequestMapping("/services")
public class ServiceCatalogViewController {

    private final ServiceCatalogService serviceCatalogService;

    public ServiceCatalogViewController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SERVICE_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW')")
    public String index(Model model) {
        model.addAttribute("services", serviceCatalogService.getAllServices());
        model.addAttribute("newService", new ServiceCatalog());
        model.addAttribute("newSla", new ServiceSla());
        return "services/list";
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('SERVICE_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW')")
    public String saveService(@ModelAttribute ServiceCatalog serviceCatalog) {
        serviceCatalogService.saveService(serviceCatalog);
        return "redirect:/services";
    }

    @PostMapping("/slas/save")
    @PreAuthorize("hasAuthority('SERVICE_EDIT') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('TENANT_VIEW')")
    public String saveSla(@RequestParam Long serviceId, @ModelAttribute ServiceSla sla) {
        serviceCatalogService.addSla(serviceId, sla);
        return "redirect:/services";
    }
}
