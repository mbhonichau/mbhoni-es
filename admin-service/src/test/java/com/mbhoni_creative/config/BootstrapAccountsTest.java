package com.mbhoni_creative.config;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BootstrapAccountsTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final MockEnvironment env = new MockEnvironment();
    private final BootstrapAccounts bootstrap = new BootstrapAccounts(users, passwords, env);
    @Test void freshInstallationRequiresExplicitCredentials() {
        assertThrows(IllegalStateException.class, () -> bootstrap.initialize(new Role(), new Role(), new Tenant()));
        verify(users, never()).save(any());
    }
    @Test void restartDoesNotResetOrReactivateExistingAdministrator() {
        when(users.countPlatformAdministrators()).thenReturn(1L);
        bootstrap.initialize(new Role(), new Role(), new Tenant());
        verify(users, never()).save(any()); verifyNoInteractions(passwords);
    }
    @Test void configuredUsernameCannotPromoteExistingTenantUser() {
        credentials(); when(users.existsByUsername("operator")).thenReturn(true);
        assertThrows(IllegalStateException.class, () -> bootstrap.initialize(new Role(), new Role(), new Tenant()));
        verify(users, never()).save(any());
    }
    @Test void explicitNewAccountGetsHashedPasswordAndRequiredRotation() {
        credentials(); when(passwords.encode("a-unique-strong-bootstrap-secret")).thenReturn("hash");
        bootstrap.initialize(new Role(), new Role(), new Tenant());
        verify(users).save(argThat(u -> u.isGlobalAdmin() && u.isPasswordChangeRequired() && "hash".equals(u.getPassword())));
    }
    private void credentials() {
        env.withProperty("app.bootstrap.admin-username", "operator").withProperty("app.bootstrap.admin-email", "operator@example.com")
                .withProperty("app.bootstrap.admin-password", "a-unique-strong-bootstrap-secret");
    }
}
