package com.mbhoni_creative.admincontroller.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mbhoni_creative.admincontroller.PublicBrandingController;
import com.mbhoni_creative.admindto.TenantBrandingResponse;
import com.mbhoni_creative.adminservice.TenantCustomizationService;

@ExtendWith(MockitoExtension.class)
class PublicBrandingControllerTest {

    @Mock
    private TenantCustomizationService customizationService;

    @InjectMocks
    private PublicBrandingController brandingController;

    @Test
    void testGetBrandingByTenantId() {
        Long tenantId = 1L;
        TenantBrandingResponse expectedResponse = new TenantBrandingResponse();
        expectedResponse.setTenantId(tenantId);
        expectedResponse.setDisplayName("Acme Portal");

        when(customizationService.getBrandingResponse(tenantId)).thenReturn(expectedResponse);

        TenantBrandingResponse response = brandingController.getBranding(tenantId);

        assertNotNull(response);
        assertEquals(tenantId, response.getTenantId());
        assertEquals("Acme Portal", response.getDisplayName());
    }
}
