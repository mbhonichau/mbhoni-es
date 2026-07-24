package com.mbhoni_creative.admincontroller;

import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.admindto.TenantBrandingResponse;
import com.mbhoni_creative.adminservice.TenantCustomizationService;

@RestController
@RequestMapping("/api/public/branding")
public class PublicBrandingController {

    private final TenantCustomizationService customizationService;

    public PublicBrandingController(TenantCustomizationService customizationService) {
        this.customizationService = customizationService;
    }

    @GetMapping("/{tenantId}")
    public TenantBrandingResponse getBranding(
            @PathVariable Long tenantId) {

        return customizationService.getBrandingResponse(tenantId);
    }
    
    @GetMapping("/domain/{domain}")
    public TenantBrandingResponse getBrandingByDomain(
            @PathVariable String domain) {

        return customizationService.getBrandingResponseByDomain(domain);
    }
}