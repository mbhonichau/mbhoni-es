package com.mbhoni_creative.admincontroller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mbhoni_creative.adminentity.LookupCategory;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantApiKey;
import com.mbhoni_creative.adminservice.MasterDataService;
import com.mbhoni_creative.config.ApiKeyPrincipal;
import com.mbhoni_creative.config.TenantAccessService;
import com.mbhoni_creative.config.TenantSecurityService;

@ExtendWith(MockitoExtension.class)
class MasterDataApiControllerTest {

    @Mock
    private MasterDataService masterDataService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void apiKeyCannotReadAnotherTenantsLookupCategory() {
        Tenant apiKeyTenant = new Tenant();
        apiKeyTenant.setId(10L);
        TenantApiKey apiKey = new TenantApiKey();
        apiKey.setTenant(apiKeyTenant);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new ApiKeyPrincipal(apiKey), null));

        Tenant categoryTenant = new Tenant();
        categoryTenant.setId(11L);
        LookupCategory category = new LookupCategory();
        category.setTenant(categoryTenant);
        when(masterDataService.getCategoryByCode("DEPARTMENT")).thenReturn(category);

        MasterDataApiController controller = new MasterDataApiController(
                masterDataService,
                new TenantAccessService(new TenantSecurityService()));

        assertThrows(AccessDeniedException.class,
                () -> controller.getActiveLookupCodes("DEPARTMENT"));

        verify(masterDataService).getCategoryByCode("DEPARTMENT");
        verifyNoMoreInteractions(masterDataService);
    }
}
