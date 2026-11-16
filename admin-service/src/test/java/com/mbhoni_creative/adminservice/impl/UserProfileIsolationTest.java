package com.mbhoni_creative.adminservice.impl;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.admindto.ProcessTogglesUpdateRequest;
import com.mbhoni_creative.adminrepository.UserRepository;
import com.mbhoni_creative.config.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserProfileIsolationTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final TenantSecurityService security = new TenantSecurityService();
    private final UserProfileServiceImpl service = new UserProfileServiceImpl(repository,
            mock(PasswordEncoder.class), new TenantAccessService(security), security);
    private User caller;
    private User target;

    @BeforeEach void setup() {
        caller = user(1L); target = user(2L);
        authenticate();
        when(repository.findById(20L)).thenReturn(Optional.of(target));
    }
    private User user(Long tenantId) {
        Tenant tenant = new Tenant(); tenant.setId(tenantId);
        User user = new User(); user.setTenant(tenant); return user;
    }
    private void authenticate() {
        CustomUserPrincipal principal = new CustomUserPrincipal(caller);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void foreignProfileCannotBeRead() {
        assertThrows(AccessDeniedException.class, () -> service.getUserProfileById(20L));
    }
    @Test void foreignSecuritySettingsCannotBeChanged() {
        ProcessTogglesUpdateRequest request = new ProcessTogglesUpdateRequest();
        request.setMfaEnforced(true);
        assertThrows(AccessDeniedException.class, () -> service.updateUserProcessToggles(20L, request));
        assertFalse(target.isMfaEnforced());
        verify(repository, never()).save(any());
    }
    @Test void ownTenantProfileCanBeRead() {
        target.setTenant(caller.getTenant());
        assertDoesNotThrow(() -> service.getUserProfileById(20L));
    }
    @Test void globalAdminCanReadForeignProfile() {
        caller.setGlobalAdmin(true); authenticate();
        assertDoesNotThrow(() -> service.getUserProfileById(20L));
    }
}
