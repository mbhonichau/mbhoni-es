package com.mbhoni_creative.admincontroller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.adminentity.OrganizationUnit;
import com.mbhoni_creative.adminservice.OrganizationUnitService;
import com.mbhoni_creative.config.TenantAccessService;

@RestController
@RequestMapping("/api/organization-units")
public class OrganizationUnitApiController {

    private final OrganizationUnitService orgUnitService;
    private final TenantAccessService tenantAccessService;

    public OrganizationUnitApiController(OrganizationUnitService orgUnitService, TenantAccessService tenantAccessService) {
        this.orgUnitService = orgUnitService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ORG_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<List<OrganizationUnit>> getOrganizationUnits(@RequestParam Long tenantId) {
        tenantAccessService.requireAccess(tenantId);
        return ResponseEntity.ok(orgUnitService.getOrganizationUnitsByTenant(tenantId));
    }

    @GetMapping("/roots")
    @PreAuthorize("hasAuthority('ORG_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<List<OrganizationUnit>> getRootUnits(@RequestParam Long tenantId) {
        tenantAccessService.requireAccess(tenantId);
        return ResponseEntity.ok(orgUnitService.getRootOrganizationUnits(tenantId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<OrganizationUnit> getUnit(@PathVariable Long id) {
        OrganizationUnit unit = orgUnitService.getOrganizationUnitById(id);
        tenantAccessService.requireAccess(unit.getTenant() != null ? unit.getTenant().getId() : null);
        return ResponseEntity.ok(unit);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORG_EDIT')")
    public ResponseEntity<OrganizationUnit> createUnit(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long parentUnitId,
            @RequestBody OrganizationUnit unit) {
        tenantAccessService.requireAccess(tenantId);
        return ResponseEntity.ok(orgUnitService.saveOrganizationUnit(tenantId, parentUnitId, unit));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG_EDIT')")
    public ResponseEntity<Void> deleteUnit(@PathVariable Long id) {
        OrganizationUnit unit = orgUnitService.getOrganizationUnitById(id);
        tenantAccessService.requireAccess(unit.getTenant() != null ? unit.getTenant().getId() : null);
        orgUnitService.deleteOrganizationUnit(id);
        return ResponseEntity.noContent().build();
    }
}
