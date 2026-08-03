package com.mbhoni_creative.admincontroller.api;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mbhoni_creative.admincontroller.IntegrationApiController;
import com.mbhoni_creative.admindto.ApiAuthStatusResponse;
import com.mbhoni_creative.admindto.IntegrationUserResponse;
import com.mbhoni_creative.admindto.PagedResponse;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantApiKey;
import com.mbhoni_creative.adminentity.User;
import com.mbhoni_creative.adminrepository.UserRepository;
import com.mbhoni_creative.config.ApiKeyPrincipal;

@ExtendWith(MockitoExtension.class)
class IntegrationApiControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private IntegrationApiController integrationApiController;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testMe() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Integration Tenant");

        TenantApiKey apiKey = new TenantApiKey();
        apiKey.setTenant(tenant);
        apiKey.setName("Integration Key");

        ApiKeyPrincipal principal = new ApiKeyPrincipal(apiKey);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        ApiAuthStatusResponse response = integrationApiController.me(auth);

        assertNotNull(response);
        assertEquals(1L, response.getTenantId());
        assertEquals("Integration Key", response.getApiKeyName());
    }

    @Test
    void testGetUsers() {
        Tenant tenant = new Tenant();
        tenant.setId(1L);

        TenantApiKey apiKey = new TenantApiKey();
        apiKey.setTenant(tenant);

        ApiKeyPrincipal principal = new ApiKeyPrincipal(apiKey);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null);
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = new User();
        user.setId(10L);
        user.setUsername("apiuser");
        user.setEmail("apiuser@example.com");

        when(userRepository.findByTenantId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        PagedResponse<IntegrationUserResponse> response = integrationApiController.users(auth, 0, 25);

        assertNotNull(response);
        assertEquals(1, response.getData().size());
        assertEquals("apiuser", response.getData().get(0).getUsername());
    }
}
