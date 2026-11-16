package com.mbhoni_creative.config;

import com.mbhoni_creative.adminentity.*;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;

class OwnerProtectionTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void ordinaryAdminCannotRemoveOrResetOwner() {
        Tenant tenant = new Tenant(); tenant.setId(1L); tenant.setOwnerUserId(7L);
        User owner = new User(); owner.setId(7L); owner.setTenant(tenant);
        User admin = new User(); admin.setId(8L); admin.setTenant(tenant);
        var principal = new CustomUserPrincipal(admin);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        assertThrows(AccessDeniedException.class, () -> OwnerProtection.requireSelfForOwner(owner));
        assertThrows(AccessDeniedException.class, () -> OwnerProtection.rejectOwnerRemoval(owner));
    }
    @Test void ownerRoleCannotBeAssignedThroughOrdinaryRoleEditor() {
        Role owner = new Role(); owner.setName("ROLE_OWNER");
        assertThrows(AccessDeniedException.class, () -> OwnerProtection.rejectOwnerAssignment(Set.of(owner)));
    }
}
