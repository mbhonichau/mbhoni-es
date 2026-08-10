package com.mbhoni_creative.admincontroller.api;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.adminentity.FieldDefinition;
import com.mbhoni_creative.adminentity.FieldSection;
import com.mbhoni_creative.adminservice.FieldDefinitionService;
import com.mbhoni_creative.config.TenantSecurityService;

@RestController
@RequestMapping("/api/employee-fields")
public class FieldDefinitionApiController {

    private final FieldDefinitionService fieldDefinitionService;
    private final TenantSecurityService tenantSecurityService;

    public FieldDefinitionApiController(
            FieldDefinitionService fieldDefinitionService,
            TenantSecurityService tenantSecurityService) {
        this.fieldDefinitionService = fieldDefinitionService;
        this.tenantSecurityService = tenantSecurityService;
    }

    @GetMapping("/{section}")
    @PreAuthorize("hasAuthority('EMPLOYEE_FIELD_SCHEMA_MANAGE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('EMPLOYEE_VIEW')")
    public ResponseEntity<List<FieldDefinition>> getFieldsForSection(@PathVariable FieldSection section) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        List<FieldDefinition> fields = fieldDefinitionService.getEffectiveSchema(tenantId, section);
        return ResponseEntity.ok(fields);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_FIELD_SCHEMA_MANAGE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public ResponseEntity<FieldDefinition> createField(@RequestBody FieldDefinition definition) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        FieldDefinition created = fieldDefinitionService.createField(tenantId, definition);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EMPLOYEE_FIELD_SCHEMA_MANAGE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public ResponseEntity<FieldDefinition> updateField(@PathVariable Long id, @RequestBody FieldDefinition definition) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        FieldDefinition updated = fieldDefinitionService.updateField(tenantId, id, definition);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('EMPLOYEE_FIELD_SCHEMA_MANAGE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public ResponseEntity<Void> deactivateField(@PathVariable Long id) {
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        fieldDefinitionService.deactivateField(tenantId, id);
        return ResponseEntity.ok().build();
    }
}
