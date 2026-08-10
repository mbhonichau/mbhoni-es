package com.mbhoni_creative.adminservice;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mbhoni_creative.adminentity.DataType;
import com.mbhoni_creative.adminentity.FieldDefinition;
import com.mbhoni_creative.adminentity.FieldSection;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminrepository.FieldDefinitionRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.impl.FieldDefinitionServiceImpl;
import com.mbhoni_creative.config.TenantSecurityService;

@ExtendWith(MockitoExtension.class)
class FieldDefinitionServiceTest {

    @Mock
    private FieldDefinitionRepository fieldDefinitionRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    @InjectMocks
    private FieldDefinitionServiceImpl fieldDefinitionService;

    private Tenant tenant;
    private FieldDefinition globalField;
    private FieldDefinition tenantField;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(10L);
        tenant.setName("Acme Logistics");

        globalField = new FieldDefinition();
        globalField.setId(1L);
        globalField.setTenant(null);
        globalField.setSection(FieldSection.QUALIFICATION);
        globalField.setFieldKey("license_number");
        globalField.setLabel("License Number (Global)");
        globalField.setDataType(DataType.TEXT);
        globalField.setActive(true);
        globalField.setDisplayOrder(10);

        tenantField = new FieldDefinition();
        tenantField.setId(2L);
        tenantField.setTenant(tenant);
        tenantField.setSection(FieldSection.QUALIFICATION);
        tenantField.setFieldKey("license_number");
        tenantField.setLabel("License Number (Custom Tenant)");
        tenantField.setDataType(DataType.TEXT);
        tenantField.setActive(true);
        tenantField.setDisplayOrder(5);
    }

    @Test
    void testGetEffectiveSchema_FallbackToGlobal_WhenNoTenantCustomization() {
        when(fieldDefinitionRepository.findActiveFieldsWithFallback(10L, FieldSection.QUALIFICATION))
                .thenReturn(List.of(globalField));

        List<FieldDefinition> result = fieldDefinitionService.getEffectiveSchema(10L, FieldSection.QUALIFICATION);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getTenant());
        assertEquals("License Number (Global)", result.get(0).getLabel());
    }

    @Test
    void testGetEffectiveSchema_ReturnsTenantSpecific_WhenCustomized() {
        when(fieldDefinitionRepository.findActiveFieldsWithFallback(10L, FieldSection.QUALIFICATION))
                .thenReturn(List.of(tenantField));

        List<FieldDefinition> result = fieldDefinitionService.getEffectiveSchema(10L, FieldSection.QUALIFICATION);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getTenant().getId());
        assertEquals("License Number (Custom Tenant)", result.get(0).getLabel());
    }

    @Test
    void testCreateField_UniquenessValidation_ThrowsExceptionWhenDuplicateKey() {
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(true);
        when(fieldDefinitionRepository.findByTenantIdAndSectionAndFieldKey(10L, FieldSection.QUALIFICATION, "license_number"))
                .thenReturn(Optional.of(tenantField));

        FieldDefinition duplicate = new FieldDefinition();
        duplicate.setSection(FieldSection.QUALIFICATION);
        duplicate.setFieldKey("license_number");
        duplicate.setLabel("Duplicate Field");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                fieldDefinitionService.createField(10L, duplicate));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(fieldDefinitionRepository, never()).save(any());
    }

    @Test
    void testDeactivateField_DeactivatesNotDeletes() {
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(true);
        when(fieldDefinitionRepository.findById(2L)).thenReturn(Optional.of(tenantField));

        fieldDefinitionService.deactivateField(10L, 2L);

        assertFalse(tenantField.isActive());
        verify(fieldDefinitionRepository).save(tenantField);
        verify(fieldDefinitionRepository, never()).delete(any());
        verify(fieldDefinitionRepository, never()).deleteById(any());
    }
}
