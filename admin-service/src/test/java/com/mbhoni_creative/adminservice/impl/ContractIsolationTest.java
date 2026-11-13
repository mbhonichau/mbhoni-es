package com.mbhoni_creative.adminservice.impl;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.config.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContractIsolationTest {
    final ContractRepository contracts = mock(ContractRepository.class);
    final ContractMilestoneRepository milestones = mock(ContractMilestoneRepository.class);
    final TenantRepository tenants = mock(TenantRepository.class);
    final ContractServiceImpl service = new ContractServiceImpl(contracts, milestones, tenants,
            new TenantAccessService(new TenantSecurityService()));
    @BeforeEach void authenticate() {
        User user = new User(); user.setTenant(tenant(1L));
        var principal = new CustomUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    static Tenant tenant(Long id) { Tenant tenant = new Tenant(); tenant.setId(id); return tenant; }
    static Contract contract(Long id, Long tenantId) {
        Contract contract = new Contract(); ReflectionTestUtils.setField(contract, "id", id); contract.setTenant(tenant(tenantId)); return contract;
    }
    @Test void foreignPayloadCannotReassignStoredContract() {
        Contract stored = contract(20L, 2L); stored.setTitle("Original");
        when(contracts.findById(20L)).thenReturn(Optional.of(stored));
        Contract payload = contract(20L, 1L); payload.setTitle("Stolen");
        assertThrows(AccessDeniedException.class, () -> service.createOrUpdateContract(1L, payload));
        assertEquals(2L, stored.getTenant().getId()); assertEquals("Original", stored.getTitle());
        verify(contracts, never()).save(any());
    }
    @Test void foreignIdNumberAndMilestoneReadsAreRejected() {
        Contract foreign = contract(20L, 2L);
        when(contracts.findById(20L)).thenReturn(Optional.of(foreign));
        when(contracts.findByContractNumber("B-1")).thenReturn(Optional.of(foreign));
        assertThrows(AccessDeniedException.class, () -> service.getContractById(20L));
        assertThrows(AccessDeniedException.class, () -> service.getContractByNumber("B-1"));
        assertThrows(AccessDeniedException.class, () -> service.getContractMilestones(20L));
        verifyNoInteractions(milestones);
    }
    @Test void omittedTenantUsesAuthenticatedTenantAndMissingIdentityFailsClosed() {
        service.getContractsByTenant(null); verify(contracts).findByTenantId(1L);
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> service.getContractsByTenant(null));
        verify(contracts, never()).findAll();
    }
    @Test void foreignMilestoneCannotBeMovedIntoOwnedContract() {
        Contract own = contract(10L, 1L); when(contracts.findById(10L)).thenReturn(Optional.of(own));
        ContractMilestone foreign = new ContractMilestone(); ReflectionTestUtils.setField(foreign, "id", 30L);
        foreign.setContract(contract(20L, 2L)); when(milestones.findById(30L)).thenReturn(Optional.of(foreign));
        ContractMilestone payload = new ContractMilestone(); ReflectionTestUtils.setField(payload, "id", 30L);
        assertThrows(AccessDeniedException.class, () -> service.addMilestone(10L, payload));
        when(tenants.findById(1L)).thenReturn(Optional.of(tenant(1L)));
        Contract nested = contract(null, 1L); nested.getMilestones().add(payload);
        assertThrows(AccessDeniedException.class, () -> service.createOrUpdateContract(1L, nested));
        verify(milestones, never()).save(any()); verify(contracts, never()).save(any());
    }
    @Test void legitimateMilestoneIsCopiedAndAttachedToAuthorizedContract() {
        Contract own = contract(10L, 1L); when(contracts.findById(10L)).thenReturn(Optional.of(own));
        when(milestones.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        ContractMilestone payload = new ContractMilestone(); payload.setTitle("Delivery"); payload.setContract(contract(20L, 2L));
        var result = service.addMilestone(10L, payload);
        assertEquals("Delivery", result.getTitle()); assertSame(own, result.getContract()); assertNotSame(payload, result);
    }
}
