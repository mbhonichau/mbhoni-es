package com.mbhoni_creative.admincontroller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.adminentity.OrganizationUnit;
import com.mbhoni_creative.adminservice.OrganizationUnitService;

@RestController
@RequestMapping("/api/organization-units")
public class OrganizationUnitApiController {

    private final OrganizationUnitService orgUnitService;

    public OrganizationUnitApiController(OrganizationUnitService orgUnitService) {
        this.orgUnitService = orgUnitService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ORG_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<List<OrganizationUnit>> getOrganizationUnits(@RequestParam Long tenantId) {
        return ResponseEntity.ok(orgUnitService.getOrganizationUnitsByTenant(tenantId));
    }

    @GetMapping("/roots")
    @PreAuthorize("hasAuthority('ORG_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<List<OrganizationUnit>> getRootUnits(@RequestParam Long tenantId) {
        return ResponseEntity.ok(orgUnitService.getRootOrganizationUnits(tenantId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<OrganizationUnit> getUnit(@PathVariable Long id) {
        return ResponseEntity.ok(orgUnitService.getOrganizationUnitById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ORG_EDIT')")
    public ResponseEntity<OrganizationUnit> createUnit(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long parentUnitId,
            @RequestBody OrganizationUnit unit) {
        return ResponseEntity.ok(orgUnitService.saveOrganizationUnit(tenantId, parentUnitId, unit));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ORG_EDIT')")
    public ResponseEntity<Void> deleteUnit(@PathVariable Long id) {
        orgUnitService.deleteOrganizationUnit(id);
        return ResponseEntity.noContent().build();
    }
}
