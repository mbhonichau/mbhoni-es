package com.mbhoni_creative.config;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.mbhoni_creative.adminentity.TenantSubscription;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.adminservice.ModuleService;

@Service("tenantEntitlementService")
public class TenantEntitlementService {

    private final TenantSecurityService tenantSecurityService;
    private final ModuleService moduleService;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;

    public TenantEntitlementService(
            TenantSecurityService tenantSecurityService,
            ModuleService moduleService,
            TenantSubscriptionRepository tenantSubscriptionRepository) {

        this.tenantSecurityService = tenantSecurityService;
        this.moduleService = moduleService;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
    }

    public boolean isModuleEnabled(String moduleCode) {
        if (tenantSecurityService.isGlobalAdmin()) {
            return true;
        }

        Long tenantId = tenantSecurityService.getCurrentTenantId();
        if (tenantId == null || moduleCode == null || moduleCode.isBlank()) {
            return false;
        }

        return moduleService.isModuleEnabled(tenantId, moduleCode);
    }

    public boolean isFeatureEnabled(String featureName) {
        if (tenantSecurityService.isGlobalAdmin()) {
            return true;
        }

        Long tenantId = tenantSecurityService.getCurrentTenantId();
        if (tenantId == null || featureName == null || featureName.isBlank()) {
            return false;
        }

        TenantSubscription subscription = tenantSubscriptionRepository.findByTenantId(tenantId)
                .orElse(null);

        if (subscription == null || subscription.getPlan() == null) {
            return false;
        }

        String feature = featureName.trim().toUpperCase(Locale.ROOT);

        return switch (feature) {
            case "BRANDING" -> subscription.getPlan().isBrandingEnabled();
            case "API_ACCESS" -> subscription.getPlan().isApiAccessEnabled();
            case "CUSTOM_DOMAIN" -> subscription.getPlan().isCustomDomainEnabled();
            default -> false;
        };
    }
}
