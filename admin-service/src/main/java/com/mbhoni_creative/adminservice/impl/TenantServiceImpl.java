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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class TenantServiceImpl implements TenantService {

private final TenantRepository tenantRepository;
private final TenantSecurityService tenantSecurityService;
private final IndustryProfileRepository industryProfileRepository;
private final NotificationService notificationService;
@PersistenceContext
private EntityManager entityManager;

public TenantServiceImpl(
        TenantRepository tenantRepository,
        TenantSecurityService tenantSecurityService,
        IndustryProfileRepository industryProfileRepository,
        NotificationService notificationService,
        EntityManager entityManager) {

    this.tenantRepository = tenantRepository;
    this.tenantSecurityService = tenantSecurityService;
    this.industryProfileRepository = industryProfileRepository;
    this.notificationService = notificationService;
    this.entityManager = entityManager;
}

// =====================================================
// GET ALL TENANTS
// =====================================================

@Override
@Transactional(readOnly = true)
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
@Transactional(readOnly = true)
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

    // Step 1: Disassociate employee manager references & delete employees
    entityManager.createQuery("UPDATE Employee e SET e.manager = NULL WHERE e.tenant.id = :id")
            .setParameter("id", id).executeUpdate();
    entityManager.createQuery("DELETE FROM Employee e WHERE e.tenant.id = :id")
            .setParameter("id", id).executeUpdate();

    // Step 2: Delete user_roles join records & user records
    entityManager.createNativeQuery("DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE tenant_id = :id)")
            .setParameter("id", id).executeUpdate();
    entityManager.createQuery("DELETE FROM User u WHERE u.tenant.id = :id")
            .setParameter("id", id).executeUpdate();

    // Step 3: Delete contract milestones & contracts
    entityManager.createNativeQuery("DELETE FROM contract_milestones WHERE contract_id IN (SELECT id FROM contracts WHERE tenant_id = :id)")
            .setParameter("id", id).executeUpdate();
    entityManager.createQuery("DELETE FROM Contract c WHERE c.tenant.id = :id")
            .setParameter("id", id).executeUpdate();

    // Step 4: Delete payments, invoices & billing accounts
    entityManager.createNativeQuery("DELETE FROM payments WHERE invoice_id IN (SELECT id FROM invoices WHERE tenant_id = :id)")
            .setParameter("id", id).executeUpdate();
    entityManager.createQuery("DELETE FROM Invoice i WHERE i.tenant.id = :id")
            .setParameter("id", id).executeUpdate();
    entityManager.createQuery("DELETE FROM BillingAccount b WHERE b.tenant.id = :id")
            .setParameter("id", id).executeUpdate();

    // Step 5: Disassociate org unit parents & delete organization units
    entityManager.createQuery("UPDATE OrganizationUnit ou SET ou.parentUnit = NULL WHERE ou.tenant.id = :id")
            .setParameter("id", id).executeUpdate();
    entityManager.createQuery("DELETE FROM OrganizationUnit ou WHERE ou.tenant.id = :id")
            .setParameter("id", id).executeUpdate();

    // Step 6: Delete lookup codes & lookup categories
    entityManager.createNativeQuery("DELETE FROM lookup_codes WHERE category_id IN (SELECT id FROM lookup_categories WHERE tenant_id = :id)")
            .setParameter("id", id).executeUpdate();
    entityManager.createQuery("DELETE FROM LookupCategory lc WHERE lc.tenant.id = :id")
            .setParameter("id", id).executeUpdate();

    // Step 7: Delete API keys & API key permissions
    entityManager.createNativeQuery("DELETE FROM tenant_api_key_permissions WHERE api_key_id IN (SELECT id FROM tenant_api_keys WHERE tenant_id = :id)")
            .setParameter("id", id).executeUpdate();
    entityManager.createQuery("DELETE FROM TenantApiKey ak WHERE ak.tenant.id = :id")
            .setParameter("id", id).executeUpdate();

    // Step 8: Delete tenant-specific settings & configurations
    entityManager.createQuery("DELETE FROM TenantSubscription ts WHERE ts.tenant.id = :id")
            .setParameter("id", id).executeUpdate();
    entityManager.createQuery("DELETE FROM TenantQuota tq WHERE tq.tenant.id = :id")
            .setParameter("id", id).executeUpdate();
    entityManager.createQuery("DELETE FROM TenantCustomization tc WHERE tc.tenant.id = :id")
            .setParameter("id", id).executeUpdate();
    entityManager.createQuery("DELETE FROM TenantModule tm WHERE tm.tenant.id = :id")
            .setParameter("id", id).executeUpdate();
    entityManager.createQuery("DELETE FROM TenantContent tc WHERE tc.tenant.id = :id")
            .setParameter("id", id).executeUpdate();
    entityManager.createQuery("DELETE FROM TenantMetric tm WHERE tm.tenant.id = :id")
            .setParameter("id", id).executeUpdate();

    // Step 9: Delete role_permissions join records & tenant roles
    entityManager.createNativeQuery("DELETE FROM role_permissions WHERE role_id IN (SELECT id FROM roles WHERE tenant_id = :id)")
            .setParameter("id", id).executeUpdate();
    entityManager.createQuery("DELETE FROM Role r WHERE r.tenant.id = :id")
            .setParameter("id", id).executeUpdate();

    // Step 10: Finally delete tenant
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
