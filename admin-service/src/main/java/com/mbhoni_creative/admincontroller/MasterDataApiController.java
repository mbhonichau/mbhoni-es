package com.mbhoni_creative.admincontroller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.adminentity.LookupCategory;
import com.mbhoni_creative.adminentity.LookupCode;
import com.mbhoni_creative.adminservice.MasterDataService;
import com.mbhoni_creative.config.TenantAccessService;

@RestController
@RequestMapping("/api/master-data")
public class MasterDataApiController {

    private final MasterDataService masterDataService;
    private final TenantAccessService tenantAccessService;

    public MasterDataApiController(MasterDataService masterDataService, TenantAccessService tenantAccessService) {
        this.masterDataService = masterDataService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('MASTER_DATA_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<List<LookupCategory>> getCategories(@RequestParam(required = false) Long tenantId) {
        return ResponseEntity.ok(masterDataService.getAllCategories(
                tenantAccessService.resolveAccessibleTenantId(tenantId)));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('MASTER_DATA_EDIT')")
    public ResponseEntity<LookupCategory> createCategory(@RequestBody LookupCategory category, @RequestParam(required = false) Long tenantId) {
        return ResponseEntity.ok(masterDataService.createCategory(
                category, tenantAccessService.resolveAccessibleTenantId(tenantId)));
    }

    @GetMapping("/lookups/{categoryCode}")
    @PreAuthorize("hasAuthority('MASTER_DATA_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<List<LookupCode>> getActiveLookupCodes(@PathVariable String categoryCode) {
        LookupCategory category = masterDataService.getCategoryByCode(categoryCode);
        tenantAccessService.requireAccessIfTenantOwned(
                category.getTenant() != null ? category.getTenant().getId() : null);
        return ResponseEntity.ok(masterDataService.getActiveLookupCodes(categoryCode));
    }

    @PostMapping("/categories/{categoryId}/codes")
    @PreAuthorize("hasAuthority('MASTER_DATA_EDIT')")
    public ResponseEntity<LookupCode> addLookupCode(@PathVariable Long categoryId, @RequestBody LookupCode code) {
        LookupCategory category = masterDataService.getCategoryById(categoryId);
        tenantAccessService.requireAccessIfTenantOwned(
                category.getTenant() != null ? category.getTenant().getId() : null);
        return ResponseEntity.ok(masterDataService.saveLookupCode(categoryId, code));
    }

    @DeleteMapping("/codes/{codeId}")
    @PreAuthorize("hasAuthority('MASTER_DATA_EDIT')")
    public ResponseEntity<Void> deleteLookupCode(@PathVariable Long codeId) {
        LookupCode code = masterDataService.getLookupCodeById(codeId);
        LookupCategory category = code.getCategory();
        tenantAccessService.requireAccessIfTenantOwned(
                category != null && category.getTenant() != null ? category.getTenant().getId() : null);
        masterDataService.deleteLookupCode(codeId);
        return ResponseEntity.noContent().build();
    }
}
