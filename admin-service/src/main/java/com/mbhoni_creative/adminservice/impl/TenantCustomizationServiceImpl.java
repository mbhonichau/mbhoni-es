package com.mbhoni_creative.adminservice.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.admindto.TenantBrandingResponse;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantCustomization;
import com.mbhoni_creative.adminentity.TenantSubscription;
import com.mbhoni_creative.adminentity.ThemeMode;
import com.mbhoni_creative.adminrepository.TenantCustomizationRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.adminservice.TenantCustomizationService;
import com.mbhoni_creative.config.TenantSecurityService;

@Service
public class TenantCustomizationServiceImpl implements TenantCustomizationService {

    private final TenantCustomizationRepository customizationRepository;
    private final TenantRepository tenantRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantSecurityService tenantSecurityService;

    public TenantCustomizationServiceImpl(
            TenantCustomizationRepository customizationRepository,
            TenantRepository tenantRepository,
            TenantSubscriptionRepository subscriptionRepository,
            TenantSecurityService tenantSecurityService) {

        this.customizationRepository = customizationRepository;
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tenantSecurityService = tenantSecurityService;
    }

    @Override
    public List<TenantCustomization> getAllCustomizations() {

        if (!tenantSecurityService.isGlobalAdmin()) {
            throw new RuntimeException("Access denied");
        }

        return customizationRepository.findAll();
    }

    @Override
    public TenantCustomization getOrCreateForTenant(Long tenantId) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        return customizationRepository.findByTenant(tenant)
                .orElseGet(() -> createDefault(tenant));
    }

    @Override
    public TenantCustomization getOrCreateForCurrentTenant() {

        Long tenantId = tenantSecurityService.getCurrentTenantId();

        if (tenantId == null) {
            throw new RuntimeException("Tenant context not set");
        }

        return getOrCreateForTenant(tenantId);
    }

    @Override
    @Transactional
    public TenantCustomization saveForTenant(Long tenantId, TenantCustomization source) {

        if (!tenantSecurityService.isGlobalAdmin()) {
            throw new RuntimeException("Access denied");
        }

        if (!brandingAllowed(tenantId)) {
            throw new RuntimeException("Branding is not enabled for this tenant's subscription plan");
        }

        TenantCustomization customization = getOrCreateForTenant(tenantId);

        apply(customization, source);

        return customizationRepository.save(customization);
    }

    @Override
    @Transactional
    public TenantCustomization saveForCurrentTenant(TenantCustomization source) {

        Long tenantId = tenantSecurityService.getCurrentTenantId();

        if (tenantId == null) {
            throw new RuntimeException("Tenant context not set");
        }

        if (!brandingAllowed(tenantId)) {
            throw new RuntimeException("Branding is not enabled for your subscription plan");
        }

        TenantCustomization customization = getOrCreateForTenant(tenantId);

        apply(customization, source);

        return customizationRepository.save(customization);
    }

    @Override
    public boolean brandingAllowed(Long tenantId) {

        TenantSubscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElse(null);

        if (subscription == null ||
            subscription.getPlan() == null) {
            return false;
        }

        return subscription.getPlan().isBrandingEnabled();
    }

    private TenantCustomization createDefault(Tenant tenant) {

        TenantCustomization customization = new TenantCustomization();

        customization.setTenant(tenant);
        customization.setDisplayName(tenant.getName());
        customization.setPrimaryColor("#2563eb");
        customization.setSecondaryColor("#16a34a");
        customization.setCountry("South Africa");
        customization.setCurrency("ZAR");
        customization.setLanguage("en");
        customization.setTimezone("Africa/Johannesburg");
        customization.setFooterText(tenant.getName());

        return customization;
    }

    private void apply(
            TenantCustomization target,
            TenantCustomization source) {

        target.setDisplayName(source.getDisplayName());
        target.setLogoUrl(source.getLogoUrl());
        target.setPrimaryColor(source.getPrimaryColor());
        target.setSecondaryColor(source.getSecondaryColor());
        target.setSupportEmail(source.getSupportEmail());
        target.setSupportPhone(source.getSupportPhone());
        target.setCustomDomain(source.getCustomDomain());
        target.setThemeMode(source.getThemeMode());
        target.setFooterText(source.getFooterText());
        target.setTimezone(source.getTimezone());
        target.setCountry(source.getCountry());
        target.setCurrency(source.getCurrency());
        target.setLanguage(source.getLanguage());
        target.setActive(source.isActive());
    }
    
    @Override
    public TenantBrandingResponse getBrandingResponse(Long tenantId) {

        TenantCustomization customization = getOrCreateForTenant(tenantId);

        boolean allowed = brandingAllowed(tenantId);

        TenantBrandingResponse response = new TenantBrandingResponse();

        response.setTenantId(customization.getTenant().getId());
        response.setTenantName(customization.getTenant().getName());
        response.setBrandingAllowed(allowed);

        if (!allowed || !customization.isActive()) {
            response.setDisplayName(customization.getTenant().getName());
            response.setPrimaryColor("#2563eb");
            response.setSecondaryColor("#16a34a");
            response.setThemeMode(ThemeMode.SYSTEM);
            response.setFooterText(customization.getTenant().getName());
            response.setTimezone("Africa/Johannesburg");
            response.setCountry("South Africa");
            response.setCurrency("ZAR");
            response.setLanguage("en");
            response.setActive(false);
            return response;
        }

        response.setDisplayName(customization.getDisplayName());
        response.setLogoUrl(customization.getLogoUrl());
        response.setPrimaryColor(customization.getPrimaryColor());
        response.setSecondaryColor(customization.getSecondaryColor());
        response.setSupportEmail(customization.getSupportEmail());
        response.setSupportPhone(customization.getSupportPhone());
        response.setCustomDomain(customization.getCustomDomain());
        response.setThemeMode(customization.getThemeMode());
        response.setFooterText(customization.getFooterText());
        response.setTimezone(customization.getTimezone());
        response.setCountry(customization.getCountry());
        response.setCurrency(customization.getCurrency());
        response.setLanguage(customization.getLanguage());
        response.setActive(customization.isActive());

        return response;
    }
    
    @Override
    public TenantBrandingResponse getBrandingResponseByDomain(String domain) {

        if (domain == null || domain.isBlank()) {
            throw new RuntimeException("Domain is required");
        }

        TenantCustomization customization = customizationRepository
                .findByCustomDomainIgnoreCase(domain.trim())
                .orElseThrow(() -> new RuntimeException("No tenant found for domain"));

        return getBrandingResponse(customization.getTenant().getId());
    }
}