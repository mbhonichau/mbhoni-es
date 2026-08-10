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

import com.mbhoni_creative.admindto.OnboardingSchemaResponse;
import com.mbhoni_creative.admindto.TenantOnboardingFieldDto;
import com.mbhoni_creative.adminentity.IndustryProfile;
import com.mbhoni_creative.adminentity.RequirementState;
import com.mbhoni_creative.adminentity.TargetEntity;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantOnboardingField;
import com.mbhoni_creative.adminrepository.IndustryProfileFieldRepository;
import com.mbhoni_creative.adminrepository.TenantOnboardingFieldRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.impl.TenantOnboardingFieldServiceImpl;
import com.mbhoni_creative.config.TenantSecurityService;

@ExtendWith(MockitoExtension.class)
class TenantOnboardingFieldServiceTest {

    @Mock
    private TenantOnboardingFieldRepository onboardingFieldRepository;

    @Mock
    private IndustryProfileFieldRepository industryFieldRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    @InjectMocks
    private TenantOnboardingFieldServiceImpl onboardingFieldService;

    private Tenant tenant;
    private TenantOnboardingField field;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(50L);
        tenant.setName("Metro Healthcare System");

        field = new TenantOnboardingField();
        field.setId(1L);
        field.setTenant(tenant);
        field.setTargetEntity(TargetEntity.EMPLOYEE);
        field.setFieldKey("nationalId");
        field.setFieldLabel("National ID / Passport");
        field.setFieldCategory("IDENTITY");
        field.setRequirementState(RequirementState.REQUIRED);
    }

    @Test
    void testGetOnboardingSchemaForTenant_Success() {
        when(tenantRepository.findById(50L)).thenReturn(Optional.of(tenant));
        when(onboardingFieldRepository.findByTenantIdOrderByDisplayOrderAsc(50L))
                .thenReturn(List.of(field));
        when(onboardingFieldRepository.findByTenantIdAndTargetEntityOrderByDisplayOrderAsc(50L, TargetEntity.USER))
                .thenReturn(List.of());
        when(onboardingFieldRepository.findByTenantIdAndTargetEntityOrderByDisplayOrderAsc(50L, TargetEntity.EMPLOYEE))
                .thenReturn(List.of(field));

        OnboardingSchemaResponse schema = onboardingFieldService.getOnboardingSchemaForTenant(50L);

        assertNotNull(schema);
        assertEquals(50L, schema.getTenantId());
        assertEquals("Metro Healthcare System", schema.getTenantName());
        assertEquals(1, schema.getEmployeeFields().size());
        assertEquals("nationalId", schema.getEmployeeFields().get(0).getFieldKey());
        assertEquals(RequirementState.REQUIRED, schema.getEmployeeFields().get(0).getRequirementState());
    }

    @Test
    void testSeedDefaultFieldsForTenant_WhenEmpty() {
        when(onboardingFieldRepository.findByTenantIdOrderByDisplayOrderAsc(50L)).thenReturn(List.of());
        when(tenantRepository.findById(50L)).thenReturn(Optional.of(tenant));

        onboardingFieldService.seedDefaultFieldsForTenant(50L);

        verify(onboardingFieldRepository).saveAll(anyList());
    }

    @Test
    void testSaveOrUpdateFields_Success() {
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(true);
        when(tenantRepository.findById(50L)).thenReturn(Optional.of(tenant));
        when(onboardingFieldRepository.findByTenantIdAndTargetEntityAndFieldKey(50L, TargetEntity.EMPLOYEE, "nationalId"))
                .thenReturn(Optional.of(field));

        TenantOnboardingFieldDto dto = new TenantOnboardingFieldDto();
        dto.setFieldKey("nationalId");
        dto.setTargetEntity(TargetEntity.EMPLOYEE);
        dto.setFieldLabel("SSN Number");
        dto.setRequirementState(RequirementState.REQUIRED);

        onboardingFieldService.saveOrUpdateFields(50L, List.of(dto));

        verify(onboardingFieldRepository).save(any(TenantOnboardingField.class));
        assertEquals("SSN Number", field.getFieldLabel());
    }
}
