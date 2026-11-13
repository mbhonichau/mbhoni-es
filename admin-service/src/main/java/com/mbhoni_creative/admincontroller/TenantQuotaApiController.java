package com.mbhoni_creative.admincontroller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantQuota;
import com.mbhoni_creative.adminrepository.TenantQuotaRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.config.TenantAccessService;

@RestController
@RequestMapping("/api/tenants/{tenantId}/quota")
public class TenantQuotaApiController {

    private final TenantQuotaRepository tenantQuotaRepository;
    private final TenantRepository tenantRepository;
    private final TenantAccessService tenantAccessService;
    private final com.mbhoni_creative.billing.TenantQuotaService quotaService;

    public TenantQuotaApiController(
            TenantQuotaRepository tenantQuotaRepository,
            TenantRepository tenantRepository,
            TenantAccessService tenantAccessService, com.mbhoni_creative.billing.TenantQuotaService quotaService) {
        this.tenantQuotaRepository = tenantQuotaRepository;
        this.tenantRepository = tenantRepository;
        this.tenantAccessService = tenantAccessService;
        this.quotaService = quotaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TENANT_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<TenantQuota> getQuota(@PathVariable Long tenantId) {
        return ResponseEntity.ok(quotaService.get(tenantId));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('TENANT_EDIT')")
    public ResponseEntity<TenantQuota> updateQuota(@PathVariable Long tenantId, @RequestBody TenantQuota quotaDetails,
            @RequestParam String reason) {
        return ResponseEntity.ok(quotaService.override(tenantId, quotaDetails, reason));
    }
}
