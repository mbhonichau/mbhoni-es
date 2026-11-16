package com.mbhoni_creative.adminservice.impl;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.config.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultsGovernanceTest {
    private final TenantRepository tenants = mock(TenantRepository.class);
    private final FieldDefinitionRepository fields = mock(FieldDefinitionRepository.class);
    private final TenantSecurityService security = new TenantSecurityService();
    private final TenantGovernance governance = new TenantGovernance(tenants, security);
    private final FieldDefinitionServiceImpl service = new FieldDefinitionServiceImpl(fields, tenants, security, governance);
    private Tenant tenant;
    private User caller;
    @BeforeEach void setup() {
        tenant = new Tenant(); tenant.setId(1L); tenant.setOwnerUserId(7L);
        caller = new User(); caller.setId(7L); caller.setTenant(tenant);
        authenticate();
        when(tenants.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenants.lockById(1L)).thenReturn(Optional.of(tenant));
        when(fields.save(any())).thenAnswer(i -> i.getArgument(0));
    }
    private void authenticate() {
        var principal = new CustomUserPrincipal(caller);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private FieldDefinition global(String key) {
        FieldDefinition f = new FieldDefinition(); f.setId(10L); f.setFieldKey(key); f.setSection(FieldSection.QUALIFICATION);
        f.setLabel("Shared"); f.setDataType(DataType.TEXT); return f;
    }
    @Test void tenantOwnerCannotMutateGlobalMaster() {
        assertThrows(AccessDeniedException.class, () -> service.createField(null, global("x")));
        verify(fields, never()).save(any());
    }
    @Test void globalEditCreatesOwnCopyAndLeavesMasterUntouched() {
        FieldDefinition global = global("license"); when(fields.findById(10L)).thenReturn(Optional.of(global));
        FieldDefinition edit = new FieldDefinition(); edit.setLabel("Custom");
        FieldDefinition saved = service.updateField(1L, 10L, edit);
        assertNotSame(global, saved); assertEquals("Shared", global.getLabel()); assertNull(global.getTenant());
        assertEquals(1L, saved.getTenant().getId()); assertEquals("Custom", saved.getLabel()); assertNull(saved.getId());
    }
    @Test void foreignFieldIdIsRejectedEvenWithOwnTenantParameter() {
        FieldDefinition foreign = global("license"); Tenant other = new Tenant(); other.setId(2L); foreign.setTenant(other);
        when(fields.findById(10L)).thenReturn(Optional.of(foreign));
        assertThrows(AccessDeniedException.class, () -> service.updateField(1L, 10L, new FieldDefinition()));
        assertThrows(AccessDeniedException.class, () -> service.deactivateField(1L, 10L));
        verify(fields, never()).save(any());
    }
    @Test void ownerNameWithoutOwnershipCannotWriteDefaults() {
        caller.setId(99L); Role fake = new Role(); fake.setName("ROLE_OWNER"); caller.setRoles(Set.of(fake)); authenticate();
        assertThrows(AccessDeniedException.class, () -> service.deactivateField(1L, 10L));
        verify(fields, never()).findById(any());
    }
    @Test void disablingGlobalFieldCreatesTenantTombstone() {
        FieldDefinition global = global("license"); when(fields.findById(10L)).thenReturn(Optional.of(global));
        service.deactivateField(1L, 10L);
        assertTrue(global.isActive());
        verify(fields).save(argThat(f -> f.getTenant() == tenant && !f.isActive()));
    }
    @Test void overlayPreservesOtherGlobalsAndAppliesTombstones() {
        when(fields.findActiveFieldsWithFallback(1L, FieldSection.QUALIFICATION)).thenCallRealMethod();
        FieldDefinition a = global("a"), b = global("b"), c = global("c");
        FieldDefinition override = global("a"); override.setTenant(tenant); override.setLabel("Customized");
        FieldDefinition removed = global("b"); removed.setTenant(tenant); removed.setActive(false);
        when(fields.findByTenantIdIsNullAndSectionAndIsActiveTrueOrderByDisplayOrderAsc(FieldSection.QUALIFICATION)).thenReturn(List.of(a,b,c));
        when(fields.findByTenantIdOrderBySectionAscDisplayOrderAsc(1L)).thenReturn(List.of(override, removed));
        assertEquals(List.of(override, c), service.getEffectiveSchema(1L, FieldSection.QUALIFICATION));
        c.setLabel("New platform label"); a.setLabel("Platform edited overridden field");
        var result = service.getEffectiveSchema(1L, FieldSection.QUALIFICATION);
        assertEquals("Customized", result.get(0).getLabel()); assertEquals("New platform label", result.get(1).getLabel());
    }
}
