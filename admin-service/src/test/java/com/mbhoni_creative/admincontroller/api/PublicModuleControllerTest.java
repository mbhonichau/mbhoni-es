package com.mbhoni_creative.admincontroller.api;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mbhoni_creative.admincontroller.PublicModuleController;
import com.mbhoni_creative.admindto.ModuleCheckResponse;
import com.mbhoni_creative.admindto.TenantModulesResponse;
import com.mbhoni_creative.adminservice.ModuleService;

@ExtendWith(MockitoExtension.class)
class PublicModuleControllerTest {

    @Mock
    private ModuleService moduleService;

    @InjectMocks
    private PublicModuleController publicModuleController;

    @Test
    void testGetTenantModules() {
        Long tenantId = 1L;
        TenantModulesResponse expectedResponse = new TenantModulesResponse();
        expectedResponse.setTenantId(tenantId);
        expectedResponse.setTenantName("Acme");

        when(moduleService.getTenantModulesResponse(tenantId)).thenReturn(expectedResponse);

        TenantModulesResponse response = publicModuleController.getTenantModules(tenantId);

        assertNotNull(response);
        assertEquals(tenantId, response.getTenantId());
        assertEquals("Acme", response.getTenantName());
    }

    @Test
    void testCheckModule() {
        Long tenantId = 1L;
        String moduleCode = "CONTRACT";

        when(moduleService.isModuleEnabled(tenantId, moduleCode)).thenReturn(true);

        ModuleCheckResponse response = publicModuleController.checkModule(tenantId, moduleCode);

        assertNotNull(response);
        assertEquals("CONTRACT", response.getModuleCode());
        assertTrue(response.isEnabled());
    }
}
