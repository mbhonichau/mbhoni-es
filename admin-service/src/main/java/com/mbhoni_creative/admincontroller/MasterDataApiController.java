package com.mbhoni_creative.admincontroller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.adminentity.LookupCategory;
import com.mbhoni_creative.adminentity.LookupCode;
import com.mbhoni_creative.adminservice.MasterDataService;

@RestController
@RequestMapping("/api/master-data")
public class MasterDataApiController {

    private final MasterDataService masterDataService;

    public MasterDataApiController(MasterDataService masterDataService) {
        this.masterDataService = masterDataService;
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAuthority('MASTER_DATA_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<List<LookupCategory>> getCategories(@RequestParam(required = false) Long tenantId) {
        return ResponseEntity.ok(masterDataService.getAllCategories(tenantId));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAuthority('MASTER_DATA_EDIT')")
    public ResponseEntity<LookupCategory> createCategory(@RequestBody LookupCategory category, @RequestParam(required = false) Long tenantId) {
        return ResponseEntity.ok(masterDataService.createCategory(category, tenantId));
    }

    @GetMapping("/lookups/{categoryCode}")
    @PreAuthorize("hasAuthority('MASTER_DATA_VIEW') or hasAuthority('API_KEY')")
    public ResponseEntity<List<LookupCode>> getActiveLookupCodes(@PathVariable String categoryCode) {
        return ResponseEntity.ok(masterDataService.getActiveLookupCodes(categoryCode));
    }

    @PostMapping("/categories/{categoryId}/codes")
    @PreAuthorize("hasAuthority('MASTER_DATA_EDIT')")
    public ResponseEntity<LookupCode> addLookupCode(@PathVariable Long categoryId, @RequestBody LookupCode code) {
        return ResponseEntity.ok(masterDataService.saveLookupCode(categoryId, code));
    }

    @DeleteMapping("/codes/{codeId}")
    @PreAuthorize("hasAuthority('MASTER_DATA_EDIT')")
    public ResponseEntity<Void> deleteLookupCode(@PathVariable Long codeId) {
        masterDataService.deleteLookupCode(codeId);
        return ResponseEntity.noContent().build();
    }
}
