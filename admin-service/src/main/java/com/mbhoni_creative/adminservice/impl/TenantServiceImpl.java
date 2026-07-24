package com.mbhoni_creative.adminservice.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.admindto.TenantDto;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.TenantService;
import com.mbhoni_creative.adminservice.NotificationService;
import com.mbhoni_creative.config.TenantSecurityService;
import com.mbhoni_creative.adminentity.IndustryProfile;
import com.mbhoni_creative.adminrepository.IndustryProfileRepository;

@Service
public class TenantServiceImpl implements TenantService {

private final TenantRepository tenantRepository;
private final TenantSecurityService tenantSecurityService;
private final IndustryProfileRepository industryProfileRepository;
private final NotificationService notificationService;

public TenantServiceImpl(
        TenantRepository tenantRepository,
        TenantSecurityService tenantSecurityService,
        IndustryProfileRepository industryProfileRepository,
        NotificationService notificationService) {

    this.tenantRepository = tenantRepository;
    this.tenantSecurityService = tenantSecurityService;
    this.industryProfileRepository = industryProfileRepository;
    this.notificationService = notificationService;
}

// =====================================================
// GET ALL TENANTS
// =====================================================

@Override
public List<TenantDto> getAllTenants() {

    assertGlobalAdmin();

    return tenantRepository.findAll()
            .stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
}

// =====================================================
// GET TENANT BY ID
// =====================================================

@Override
public TenantDto getTenantById(Long id) {

    Tenant tenant = tenantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tenant not found"));

    assertTenantAccess(tenant);

    return mapToDto(tenant);
}

// =====================================================
// CREATE TENANT
// =====================================================

@Override
@Transactional
public TenantDto saveTenant(TenantDto dto) {

    assertGlobalAdmin();

    Tenant tenant = new Tenant();

    tenant.setName(dto.getName());
    tenant.setActive(dto.isActive());
    tenant.setIndustryProfile(resolveIndustryProfile(dto.getIndustryProfileId()));

    Tenant savedTenant = tenantRepository.save(tenant);

    return mapToDto(savedTenant);
}

// =====================================================
// UPDATE TENANT
// =====================================================

@Override
@Transactional
public TenantDto updateTenant(Long id, TenantDto dto) {

    Tenant tenant = tenantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tenant not found"));

    assertTenantAccess(tenant);

    boolean wasActive = tenant.isActive();
    
    tenant.setName(dto.getName());
    tenant.setActive(dto.isActive());
    tenant.setIndustryProfile(resolveIndustryProfile(dto.getIndustryProfileId()));

    Tenant updatedTenant = tenantRepository.save(tenant);

    if (wasActive && !dto.isActive()) {
        notificationService.sendTenantSuspensionAlert(tenant.getName(), "Deactivated by admin");
    }

    return mapToDto(updatedTenant);
}

// =====================================================
// DELETE TENANT
// =====================================================

@Override
@Transactional
public void deleteTenant(Long id) {

    assertGlobalAdmin();

    Tenant tenant = tenantRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Tenant not found"));

    tenantRepository.delete(tenant);
}

// =====================================================
// DTO MAPPING
// =====================================================

private TenantDto mapToDto(Tenant tenant) {

    TenantDto dto = new TenantDto();

    dto.setId(tenant.getId());
    dto.setName(tenant.getName());
    dto.setActive(tenant.isActive());

    // Placeholder values until TenantMetric is implemented
    dto.setLatestUsage("0");
    dto.setLastUpdated(null);
    
    if (tenant.getIndustryProfile() != null) {
        dto.setIndustryProfileId(tenant.getIndustryProfile().getId());
        dto.setIndustryProfileName(tenant.getIndustryProfile().getName());
    }

    return dto;
}

private IndustryProfile resolveIndustryProfile(Long industryProfileId) {

    if (industryProfileId == null) {
        return null;
    }

    return industryProfileRepository.findById(industryProfileId)
            .orElseThrow(() -> new RuntimeException("Industry profile not found"));
}

// =====================================================
// SECURITY HELPERS
// =====================================================

private void assertGlobalAdmin() {

    if (!tenantSecurityService.isGlobalAdmin()) {
        throw new RuntimeException("Access denied: global admin only");
    }
}

private void assertTenantAccess(Tenant tenant) {

    if (!tenantSecurityService.isGlobalAdmin()) {
        throw new RuntimeException("Access denied");
    }
}

}
