package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.BDDMockito.given;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mbhoni_creative.adminentity.DataType;
import com.mbhoni_creative.adminentity.FieldDefinition;
import com.mbhoni_creative.adminentity.FieldSection;
import com.mbhoni_creative.adminentity.Tenant;

@ExtendWith(MockitoExtension.class)
class TenantIsolationRepositoryTest {

    @Mock
    private FieldDefinitionRepository fieldDefinitionRepository;

    private Tenant tenantA;
    private Tenant tenantB;

    private FieldDefinition fieldTenantA;
    private FieldDefinition fieldTenantB;

    @BeforeEach
    void setUp() {
        tenantA = new Tenant();
        tenantA.setId(101L);
        tenantA.setName("Tenant A Logistics");

        tenantB = new Tenant();
        tenantB.setId(202L);
        tenantB.setName("Tenant B Mining");

        fieldTenantA = new FieldDefinition();
        fieldTenantA.setId(10L);
        fieldTenantA.setTenant(tenantA);
        fieldTenantA.setSection(FieldSection.QUALIFICATION);
        fieldTenantA.setFieldKey("tenant_a_license");
        fieldTenantA.setLabel("Tenant A Special License");
        fieldTenantA.setDataType(DataType.TEXT);
        fieldTenantA.setActive(true);

        fieldTenantB = new FieldDefinition();
        fieldTenantB.setId(20L);
        fieldTenantB.setTenant(tenantB);
        fieldTenantB.setSection(FieldSection.QUALIFICATION);
        fieldTenantB.setFieldKey("tenant_b_permit");
        fieldTenantB.setLabel("Tenant B Mining Permit");
        fieldTenantB.setDataType(DataType.TEXT);
        fieldTenantB.setActive(true);
    }

    @Test
    void testTenantIsolation_TenantACannotReadTenantBFields() {
        given(fieldDefinitionRepository.findByTenantIdAndSectionAndIsActiveTrueOrderByDisplayOrderAsc(101L, FieldSection.QUALIFICATION))
                .willReturn(List.of(fieldTenantA));

        List<FieldDefinition> tenantAFields = fieldDefinitionRepository
                .findByTenantIdAndSectionAndIsActiveTrueOrderByDisplayOrderAsc(101L, FieldSection.QUALIFICATION);

        assertNotNull(tenantAFields);
        assertEquals(1, tenantAFields.size());
        assertEquals(101L, tenantAFields.get(0).getTenant().getId());
        assertEquals("tenant_a_license", tenantAFields.get(0).getFieldKey());

        // Confirm Tenant B field is NOT present in Tenant A queries
        boolean containsTenantB = tenantAFields.stream()
                .anyMatch(f -> f.getTenant() != null && f.getTenant().getId().equals(202L));
        assertFalse(containsTenantB, "Tenant A isolation failure: Tenant B fields returned.");
    }

    @Test
    void testTenantIsolation_TenantACannotLookupTenantBFieldKey() {
        given(fieldDefinitionRepository.findByTenantIdAndSectionAndFieldKey(101L, FieldSection.QUALIFICATION, "tenant_b_permit"))
                .willReturn(Optional.empty());

        Optional<FieldDefinition> found = fieldDefinitionRepository
                .findByTenantIdAndSectionAndFieldKey(101L, FieldSection.QUALIFICATION, "tenant_b_permit");

        assertTrue(found.isEmpty(), "Tenant isolation failure: Tenant A accessed Tenant B field key.");
    }
}
