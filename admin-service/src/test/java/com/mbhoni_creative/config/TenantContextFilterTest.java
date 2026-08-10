package com.mbhoni_creative.config;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.User;
import com.mbhoni_creative.adminrepository.UserRepository;
import com.mbhoni_creative.security.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class TenantContextFilterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TenantContextFilter filter;

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void ignoresTenantHeaderWithoutAuthenticatedGlobalAdmin() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            assertNull(TenantContext.getTenantId());
        });

        assertNull(TenantContext.getTenantId());
    }

    @Test
    void resolvesTenantHeaderForGlobalAdmin() throws ServletException, IOException {
        User user = new User();
        user.setUsername("admin");
        user.setGlobalAdmin(true);
        CustomUserPrincipal principal = new CustomUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        lenient().when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) ->
                assertEquals(42L, TenantContext.getTenantId()));

        assertNull(TenantContext.getTenantId());
    }

    @Test
    void testCustomUserPrincipalTenantResolution() throws ServletException, IOException {
        Tenant tenant = new Tenant();
        tenant.setId(10L);

        User user = new User();
        user.setUsername("tenantuser");
        user.setTenant(tenant);
        user.setGlobalAdmin(false);

        CustomUserPrincipal principal = new CustomUserPrincipal(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        lenient().when(userRepository.findByUsername(anyString())).thenReturn(Optional.of(user));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            assertEquals(10L, TenantContext.getTenantId());
        });

        assertNull(TenantContext.getTenantId());
    }
}
