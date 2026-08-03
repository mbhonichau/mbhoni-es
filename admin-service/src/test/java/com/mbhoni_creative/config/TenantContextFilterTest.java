package com.mbhoni_creative.config;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.User;
import com.mbhoni_creative.security.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

@ExtendWith(MockitoExtension.class)
class TenantContextFilterTest {

    private final TenantContextFilter filter = new TenantContextFilter();

    @Mock
    private FilterChain filterChain;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void testXTenantIdHeaderResolution() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Tenant-ID", "42");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            assertEquals(42L, TenantContext.getTenantId());
        });

        assertNull(TenantContext.getTenantId());
    }

    @Test
    void testCustomUserPrincipalTenantResolution() throws ServletException, IOException {
        Tenant tenant = new Tenant();
        tenant.setId(10L);

        User user = new User();
        user.setTenant(tenant);
        user.setGlobalAdmin(false);

        CustomUserPrincipal principal = new CustomUserPrincipal(user);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            assertEquals(10L, TenantContext.getTenantId());
        });

        assertNull(TenantContext.getTenantId());
    }
}
