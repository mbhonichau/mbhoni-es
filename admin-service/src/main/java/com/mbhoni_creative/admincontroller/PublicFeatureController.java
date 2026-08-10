package com.mbhoni_creative.admincontroller;

import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.admindto.TenantFeatureResponse;
import com.mbhoni_creative.adminentity.SubscriptionStatus;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantSubscription;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;

import com.mbhoni_creative.adminservice.TenantSettingsService;

@RestController
@RequestMapping("/api/public/features")
public class PublicFeatureController {

    private final TenantRepository tenantRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantSettingsService settingsService;

    public PublicFeatureController(
            TenantRepository tenantRepository,
            TenantSubscriptionRepository subscriptionRepository,
            TenantSettingsService settingsService) {

        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.settingsService = settingsService;
    }

    @GetMapping("/{tenantId}")
    public TenantFeatureResponse getFeatures(
            @PathVariable Long tenantId) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        TenantFeatureResponse response = new TenantFeatureResponse();

        response.setTenantId(tenant.getId());
        response.setTenantName(tenant.getName());
        response.setAdminSettings(settingsService.getSettingsForTenant(tenantId));

        TenantSubscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElse(null);

        if (subscription == null || subscription.getPlan() == null) {
            response.setSubscriptionStatus(null);
            response.setActiveSubscription(false);
            response.setSuspended(false);
            response.setCancelled(false);
            return response;
        }

        response.setSubscriptionStatus(subscription.getStatus());
        response.setPlanCode(subscription.getPlan().getCode());
        response.setPlanName(subscription.getPlan().getName());
        response.setMaxUsers(subscription.getPlan().getMaxUsers());
        response.setMaxStorageMb(subscription.getPlan().getMaxStorageMb());
        response.setApiAccessEnabled(subscription.getPlan().isApiAccessEnabled());
        response.setBrandingEnabled(subscription.getPlan().isBrandingEnabled());
        response.setCustomDomainEnabled(subscription.getPlan().isCustomDomainEnabled());

        response.setActiveSubscription(subscription.getStatus() == SubscriptionStatus.ACTIVE);
        response.setSuspended(subscription.getStatus() == SubscriptionStatus.SUSPENDED);
        response.setCancelled(subscription.getStatus() == SubscriptionStatus.CANCELLED);

        return response;
    }
}