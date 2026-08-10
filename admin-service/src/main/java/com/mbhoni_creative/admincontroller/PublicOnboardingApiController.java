package com.mbhoni_creative.admincontroller;

import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.admindto.OnboardingSchemaResponse;
import com.mbhoni_creative.adminservice.TenantOnboardingFieldService;

@RestController
@RequestMapping("/api/public/onboarding-schema")
public class PublicOnboardingApiController {

    private final TenantOnboardingFieldService onboardingFieldService;

    public PublicOnboardingApiController(TenantOnboardingFieldService onboardingFieldService) {
        this.onboardingFieldService = onboardingFieldService;
    }

    @GetMapping("/{tenantId}")
    public OnboardingSchemaResponse getOnboardingSchema(@PathVariable Long tenantId) {
        return onboardingFieldService.getOnboardingSchemaForTenant(tenantId);
    }
}
