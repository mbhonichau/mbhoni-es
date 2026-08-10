package com.mbhoni_creative.admincontroller.api;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mbhoni_creative.admincontroller.PublicFeatureController;
import com.mbhoni_creative.admindto.TenantFeatureResponse;
import com.mbhoni_creative.admindto.TenantSettingsDto;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.adminservice.TenantSettingsService;

@ExtendWith(MockitoExtension.class)
class PublicFeatureControllerTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantSubscriptionRepository subscriptionRepository;

    @Mock
    private TenantSettingsService settingsService;

    @InjectMocks
    private PublicFeatureController publicFeatureController;

    @Test
    void testGetFeatures() {
        Long tenantId = 1L;
        Tenant tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Acme Tenant");

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(settingsService.getSettingsForTenant(tenantId)).thenReturn(new TenantSettingsDto());

        TenantFeatureResponse response = publicFeatureController.getFeatures(tenantId);

        assertNotNull(response);
        assertEquals(tenantId, response.getTenantId());
        assertEquals("Acme Tenant", response.getTenantName());
        assertNotNull(response.getAdminSettings());
    }
}
