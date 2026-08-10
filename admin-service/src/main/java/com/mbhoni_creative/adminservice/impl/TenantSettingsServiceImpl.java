package com.mbhoni_creative.adminservice.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.admindto.TenantSettingsDto;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantSettings;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.TenantSettingsRepository;
import com.mbhoni_creative.adminservice.TenantSettingsService;
import com.mbhoni_creative.config.TenantSecurityService;

@Service
public class TenantSettingsServiceImpl implements TenantSettingsService {

    private final TenantSettingsRepository settingsRepository;
    private final TenantRepository tenantRepository;
    private final TenantSecurityService tenantSecurityService;

    public TenantSettingsServiceImpl(
            TenantSettingsRepository settingsRepository,
            TenantRepository tenantRepository,
            TenantSecurityService tenantSecurityService) {
        this.settingsRepository = settingsRepository;
        this.tenantRepository = tenantRepository;
        this.tenantSecurityService = tenantSecurityService;
    }

    @Override
    @Transactional
    public TenantSettings getOrCreateEntityForTenant(Long tenantId) {
        return settingsRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    Tenant tenant = tenantRepository.findById(tenantId)
                            .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));

                    TenantSettings settings = new TenantSettings();
                    settings.setTenant(tenant);
                    return settingsRepository.save(settings);
                });
    }

    @Override
    @Transactional
    public TenantSettingsDto getSettingsForTenant(Long tenantId) {
        assertAccess(tenantId);
        TenantSettings settings = getOrCreateEntityForTenant(tenantId);
        return mapToDto(settings);
    }

    @Override
    @Transactional
    public TenantSettingsDto getSettingsForCurrentTenant() {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("No active tenant context found.");
        }
        return getSettingsForTenant(tenantId);
    }

    @Override
    @Transactional
    public TenantSettingsDto updateSettingsForTenant(Long tenantId, TenantSettingsDto dto) {
        assertAccess(tenantId);
        TenantSettings settings = getOrCreateEntityForTenant(tenantId);

        // Security & Authentication Governance
        settings.setMfaEnforced(dto.isMfaEnforced());
        settings.setSsoEnforced(dto.isSsoEnforced());
        if (dto.getPasswordRotationDays() != null) settings.setPasswordRotationDays(dto.getPasswordRotationDays());
        if (dto.getMaxFailedLoginAttempts() != null) settings.setMaxFailedLoginAttempts(dto.getMaxFailedLoginAttempts());
        if (dto.getSessionTimeoutMinutes() != null) settings.setSessionTimeoutMinutes(dto.getSessionTimeoutMinutes());
        settings.setIpWhitelistEnabled(dto.isIpWhitelistEnabled());
        settings.setAllowedIpRanges(dto.getAllowedIpRanges());

        // Audit, Logging & Compliance Governance
        settings.setAuditExtendedLogging(dto.isAuditExtendedLogging());
        if (dto.getDataRetentionDays() != null) settings.setDataRetentionDays(dto.getDataRetentionDays());
        settings.setGdprComplianceMode(dto.isGdprComplianceMode());
        settings.setComplianceExportEnabled(dto.isComplianceExportEnabled());

        // API & Integration Administration
        settings.setApiAccessEnabled(dto.isApiAccessEnabled());
        settings.setWebhookEventsEnabled(dto.isWebhookEventsEnabled());
        if (dto.getRateLimitPerMinute() != null) settings.setRateLimitPerMinute(dto.getRateLimitPerMinute());
        settings.setCustomDomainEnabled(dto.isCustomDomainEnabled());

        // Workspace Operations & Self-Service
        settings.setMaintenanceMode(dto.isMaintenanceMode());
        settings.setSelfRegistrationAllowed(dto.isSelfRegistrationAllowed());
        settings.setAllowCustomRoles(dto.isAllowCustomRoles());
        settings.setMultiCurrencyEnabled(dto.isMultiCurrencyEnabled());
        if (dto.getMaxFileUploadMb() != null) settings.setMaxFileUploadMb(dto.getMaxFileUploadMb());

        // Subscription & Billing Entitlement
        settings.setOverageAllowed(dto.isOverageAllowed());
        settings.setAutoRenewalEnabled(dto.isAutoRenewalEnabled());

        TenantSettings saved = settingsRepository.save(settings);
        return mapToDto(saved);
    }

    @Override
    @Transactional
    public TenantSettingsDto updateSettingsForCurrentTenant(TenantSettingsDto dto) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        if (tenantId == null) {
            throw new RuntimeException("No active tenant context found.");
        }
        return updateSettingsForTenant(tenantId, dto);
    }

    private TenantSettingsDto mapToDto(TenantSettings settings) {
        TenantSettingsDto dto = new TenantSettingsDto();
        dto.setId(settings.getId());
        if (settings.getTenant() != null) {
            dto.setTenantId(settings.getTenant().getId());
            dto.setTenantName(settings.getTenant().getName());
        }

        // Security & Authentication Governance
        dto.setMfaEnforced(settings.isMfaEnforced());
        dto.setSsoEnforced(settings.isSsoEnforced());
        dto.setPasswordRotationDays(settings.getPasswordRotationDays());
        dto.setMaxFailedLoginAttempts(settings.getMaxFailedLoginAttempts());
        dto.setSessionTimeoutMinutes(settings.getSessionTimeoutMinutes());
        dto.setIpWhitelistEnabled(settings.isIpWhitelistEnabled());
        dto.setAllowedIpRanges(settings.getAllowedIpRanges());

        // Audit, Logging & Compliance Governance
        dto.setAuditExtendedLogging(settings.isAuditExtendedLogging());
        dto.setDataRetentionDays(settings.getDataRetentionDays());
        dto.setGdprComplianceMode(settings.isGdprComplianceMode());
        dto.setComplianceExportEnabled(settings.isComplianceExportEnabled());

        // API & Integration Administration
        dto.setApiAccessEnabled(settings.isApiAccessEnabled());
        dto.setWebhookEventsEnabled(settings.isWebhookEventsEnabled());
        dto.setRateLimitPerMinute(settings.getRateLimitPerMinute());
        dto.setCustomDomainEnabled(settings.isCustomDomainEnabled());

        // Workspace Operations & Self-Service
        dto.setMaintenanceMode(settings.isMaintenanceMode());
        dto.setSelfRegistrationAllowed(settings.isSelfRegistrationAllowed());
        dto.setAllowCustomRoles(settings.isAllowCustomRoles());
        dto.setMultiCurrencyEnabled(settings.isMultiCurrencyEnabled());
        dto.setMaxFileUploadMb(settings.getMaxFileUploadMb());

        // Subscription & Billing Entitlement
        dto.setOverageAllowed(settings.isOverageAllowed());
        dto.setAutoRenewalEnabled(settings.isAutoRenewalEnabled());

        return dto;
    }

    private void assertAccess(Long tenantId) {
        if (tenantSecurityService.isGlobalAdmin()) {
            return;
        }
        Long currentTenantId = tenantSecurityService.getCurrentTenantId();
        if (currentTenantId == null || !currentTenantId.equals(tenantId)) {
            throw new RuntimeException("Access denied: You do not have permission to manage settings for tenant ID " + tenantId);
        }
    }
}
