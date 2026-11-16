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
import com.mbhoni_creative.adminservice.AuditLogService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final AuditLogService auditLogService;
    private final com.mbhoni_creative.billing.TenantPurgeService purge;
    @PersistenceContext
    private EntityManager entityManager;

    public TenantServiceImpl(
            TenantRepository tenantRepository,
            TenantSecurityService tenantSecurityService,
            IndustryProfileRepository industryProfileRepository,
            NotificationService notificationService,
            EntityManager entityManager, AuditLogService auditLogService, com.mbhoni_creative.billing.TenantPurgeService purge) {

        this.tenantRepository = tenantRepository;
        this.tenantSecurityService = tenantSecurityService;
        this.industryProfileRepository = industryProfileRepository;
        this.notificationService = notificationService;
        this.entityManager = entityManager;
        this.auditLogService = auditLogService;
        this.purge = purge;
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!tenantSecurityService.isGlobalAdmin() && (authentication == null ||
                authentication.getAuthorities().stream().noneMatch(a -> "TENANT_EDIT".equals(a.getAuthority())))) {
            throw new AccessDeniedException("Tenant editing permission is required");
        }

        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        assertTenantAccess(tenant);

        boolean wasActive = tenant.isActive();
        if (wasActive != dto.isActive()) {
            if (!tenantSecurityService.isGlobalAdmin()) {
                throw new AccessDeniedException("Only platform administrators may override tenant activation");
            }
            if (dto.getChangeReason() == null || dto.getChangeReason().isBlank()) {
                throw new IllegalArgumentException("A reason is required for activation overrides");
            }
        }
        
        tenant.setName(dto.getName());
        tenant.setActive(dto.isActive());
        tenant.setIndustryProfile(resolveIndustryProfile(dto.getIndustryProfileId()));

        Tenant updatedTenant = tenantRepository.save(tenant);

        if (wasActive != dto.isActive()) {
            auditLogService.logForTenant(id, "TENANT_ACTIVATION_OVERRIDE", "TENANT", String.valueOf(id),
                    "Activation changed from " + wasActive + " to " + dto.isActive() + ": " + dto.getChangeReason().trim());
        }

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
        if (!purge.purgeExpired(id))
            throw new IllegalStateException("Tenant must be CANCELLED and its export retention expired before deletion");
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
            throw new AccessDeniedException("Access denied: global admin only");
        }
    }

    private void assertTenantAccess(Tenant tenant) {

        if (tenantSecurityService.isGlobalAdmin()) {
            return;
        }

        Long currentTenantId = tenantSecurityService.getCurrentTenantId();
        if (currentTenantId == null || !currentTenantId.equals(tenant.getId())) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
