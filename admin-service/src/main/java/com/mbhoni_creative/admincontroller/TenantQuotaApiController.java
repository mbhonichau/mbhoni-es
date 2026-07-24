package com.mbhoni_creative.admincontroller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantQuota;
import com.mbhoni_creative.adminrepository.TenantQuotaRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;

@RestController
@RequestMapping("/api/tenants/{tenantId}/quota")
public class TenantQuotaApiController {

    private final TenantQuotaRepository tenantQuotaRepository;
    private final TenantRepository tenantRepository;

    public TenantQuotaApiController(
            TenantQuotaRepository tenantQuotaRepository,
            TenantRepository tenantRepository) {
        this.tenantQuotaRepository = tenantQuotaRepository;
        this.tenantRepository = tenantRepository;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TENANT_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<TenantQuota> getQuota(@PathVariable Long tenantId) {
        TenantQuota quota = tenantQuotaRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    Tenant tenant = tenantRepository.findById(tenantId)
                            .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));
                    TenantQuota newQuota = new TenantQuota();
                    newQuota.setTenant(tenant);
                    return tenantQuotaRepository.save(newQuota);
                });
        return ResponseEntity.ok(quota);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('TENANT_EDIT')")
    public ResponseEntity<TenantQuota> updateQuota(@PathVariable Long tenantId, @RequestBody TenantQuota quotaDetails) {
        TenantQuota quota = tenantQuotaRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    Tenant tenant = tenantRepository.findById(tenantId)
                            .orElseThrow(() -> new RuntimeException("Tenant not found with ID: " + tenantId));
                    TenantQuota newQuota = new TenantQuota();
                    newQuota.setTenant(tenant);
                    return newQuota;
                });

        if (quotaDetails.getMaxUsers() != null) quota.setMaxUsers(quotaDetails.getMaxUsers());
        if (quotaDetails.getMaxApiRequestsPerMinute() != null) quota.setMaxApiRequestsPerMinute(quotaDetails.getMaxApiRequestsPerMinute());
        if (quotaDetails.getMaxStorageGb() != null) quota.setMaxStorageGb(quotaDetails.getMaxStorageGb());
        if (quotaDetails.getCustomQuotasJson() != null) quota.setCustomQuotasJson(quotaDetails.getCustomQuotasJson());

        return ResponseEntity.ok(tenantQuotaRepository.save(quota));
    }
}
