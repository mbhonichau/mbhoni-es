package com.mbhoni_creative.config;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.UserRepository;
import com.mbhoni_creative.onboarding.TenantSignupService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OAuthMembershipTest {
    private final UserRepository users = mock(UserRepository.class);
    private final TenantSignupService signup = mock(TenantSignupService.class);
    private final CustomOAuth2UserService service = new CustomOAuth2UserService(users, signup);
    private Map<String, Object> claims(boolean verified) {
        return Map.of("email", "new@example.com", "email_verified", verified, "sub", "subject1", "name", "New");
    }
    @Test void unverifiedEmailCannotJoinOrCreateTenant() {
        assertThrows(OAuth2AuthenticationException.class, () -> service.resolveVerifiedGoogle(claims(false)));
        verifyNoInteractions(users, signup);
    }
    @Test void newEmailProvisionsOwnTenantAndReturnsTenantAwarePrincipal() {
        Tenant tenant = new Tenant(); tenant.setId(42L);
        User user = new User(); user.setTenant(tenant); user.setUsername("new@example.com");
        when(signup.signupGoogle("new@example.com", "subject1", "New")).thenReturn(user);
        TenantOAuthPrincipal principal = (TenantOAuthPrincipal) service.resolveVerifiedGoogle(claims(true));
        assertEquals(42L, principal.getTenantId()); assertFalse(principal.isGlobalAdmin());
        verify(users, never()).findAll();
    }
    @Test void emailMatchCannotTakeOverLocalAccount() {
        User user = new User(); user.setAuthProvider("LOCAL");
        when(users.findByEmail("new@example.com")).thenReturn(Optional.of(user));
        assertThrows(OAuth2AuthenticationException.class, () -> service.resolveVerifiedGoogle(claims(true)));
        assertEquals("LOCAL", user.getAuthProvider()); verifyNoInteractions(signup);
    }
    @Test void differentSubjectAndInactiveAccountsAreRejected() {
        User user = new User(); user.setAuthProvider("GOOGLE"); user.setProviderId("different");
        when(users.findByEmail("new@example.com")).thenReturn(Optional.of(user));
        assertThrows(OAuth2AuthenticationException.class, () -> service.resolveVerifiedGoogle(claims(true)));
        user.setProviderId("subject1"); user.setActive(false);
        assertThrows(OAuth2AuthenticationException.class, () -> service.resolveVerifiedGoogle(claims(true)));
        verifyNoInteractions(signup);
    }
}
