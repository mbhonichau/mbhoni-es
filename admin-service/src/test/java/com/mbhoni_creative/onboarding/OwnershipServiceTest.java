package com.mbhoni_creative.onboarding;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.AuditLogService;
import com.mbhoni_creative.config.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OwnershipServiceTest {
    final TenantRepository tenants = mock(TenantRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final RoleRepository roles = mock(RoleRepository.class);
    final AuditLogService audit = mock(AuditLogService.class);
    final TenantSecurityService security = new TenantSecurityService();
    final OwnershipService service = new OwnershipService(tenants, users, roles, new TenantGovernance(tenants, security), security, audit);
    Tenant tenant; User owner, next; Role role;
    @BeforeEach void setup() {
        tenant = new Tenant(); tenant.setId(1L); tenant.setOwnerUserId(10L);
        owner = user(10L); next = user(11L); role = new Role(); role.setName("ROLE_OWNER"); role.setTenant(tenant); owner.getRoles().add(role);
        when(tenants.lockById(1L)).thenReturn(Optional.of(tenant)); when(tenants.findById(1L)).thenReturn(Optional.of(tenant));
        when(users.findByIdAndTenantId(10L, 1L)).thenReturn(Optional.of(owner)); when(users.findByIdAndTenantId(11L, 1L)).thenReturn(Optional.of(next));
        when(roles.findByNameAndTenantId("ROLE_OWNER", 1L)).thenReturn(Optional.of(role)); authenticate(owner);
    }
    User user(Long id) { User user = new User(); user.setId(id); user.setTenant(tenant); user.setActive(true); return user; }
    void authenticate(User user) { var principal = new CustomUserPrincipal(user); SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void transferMovesUniqueOwnerAndAuditsActorReason() {
        service.transfer(1L, 11L, "Approved transfer");
        assertEquals(11L, tenant.getOwnerUserId()); assertFalse(owner.getRoles().contains(role)); assertTrue(next.getRoles().contains(role));
        verify(audit).logForTenant(1L, "OWNERSHIP_TRANSFER", "TENANT", "1", "10 -> 11: Approved transfer");
    }
    @Test void ownerRoleNameCannotSubstituteForRecordedOwner() {
        next.getRoles().add(role); authenticate(next);
        assertThrows(AccessDeniedException.class, () -> service.transfer(1L, 11L, "Self elevation"));
        assertEquals(10L, tenant.getOwnerUserId()); verify(tenants, never()).save(any());
    }
    @Test void missingReasonForeignUserOrInactiveUserCannotReceiveOwnership() {
        assertThrows(IllegalArgumentException.class, () -> service.transfer(1L, 11L, " "));
        assertThrows(NoSuchElementException.class, () -> service.transfer(1L, 99L, "Foreign user"));
        next.setActive(false); assertThrows(NoSuchElementException.class, () -> service.transfer(1L, 11L, "Inactive user"));
        assertEquals(10L, tenant.getOwnerUserId()); verifyNoInteractions(audit);
    }
    @Test void platformOverrideUsesDistinctAuditedAction() {
        User platform = new User(); platform.setGlobalAdmin(true); authenticate(platform);
        service.transfer(1L, 11L, "Support case 123");
        verify(audit).logForTenant(1L, "PLATFORM_OWNERSHIP_OVERRIDE", "TENANT", "1", "10 -> 11: Support case 123");
    }
}
