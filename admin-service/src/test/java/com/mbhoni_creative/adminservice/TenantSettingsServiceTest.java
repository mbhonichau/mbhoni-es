package com.mbhoni_creative.adminservice;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mbhoni_creative.admindto.TenantSettingsDto;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantSettings;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.TenantSettingsRepository;
import com.mbhoni_creative.adminservice.impl.TenantSettingsServiceImpl;
import com.mbhoni_creative.config.TenantSecurityService;

@ExtendWith(MockitoExtension.class)
class TenantSettingsServiceTest {

    @Mock
    private TenantSettingsRepository settingsRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    @InjectMocks
    private TenantSettingsServiceImpl tenantSettingsService;

    private Tenant tenant;
    private TenantSettings settings;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(100L);
        tenant.setName("Apex Global Logistics");

        settings = new TenantSettings();
        settings.setId(10L);
        settings.setTenant(tenant);
        settings.setMfaEnforced(true);
        settings.setSsoEnforced(false);
        settings.setPasswordRotationDays(90);
        settings.setRateLimitPerMinute(2000);
    }

    @Test
    void testGetSettingsForTenant_Success() {
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(true);
        when(settingsRepository.findByTenantId(100L)).thenReturn(Optional.of(settings));

        TenantSettingsDto dto = tenantSettingsService.getSettingsForTenant(100L);

        assertNotNull(dto);
        assertEquals(100L, dto.getTenantId());
        assertEquals("Apex Global Logistics", dto.getTenantName());
        assertTrue(dto.isMfaEnforced());
        assertFalse(dto.isSsoEnforced());
        assertEquals(90, dto.getPasswordRotationDays());
        assertEquals(2000, dto.getRateLimitPerMinute());
    }

    @Test
    void testGetSettingsForTenant_CreatesDefaultIfNotFound() {
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(true);
        when(settingsRepository.findByTenantId(100L)).thenReturn(Optional.empty());
        when(tenantRepository.findById(100L)).thenReturn(Optional.of(tenant));
        when(settingsRepository.save(any(TenantSettings.class))).thenAnswer(i -> {
            TenantSettings s = i.getArgument(0);
            s.setId(11L);
            return s;
        });

        TenantSettingsDto dto = tenantSettingsService.getSettingsForTenant(100L);

        assertNotNull(dto);
        assertEquals(100L, dto.getTenantId());
        assertFalse(dto.isMfaEnforced());
        assertEquals(90, dto.getPasswordRotationDays());
        verify(settingsRepository).save(any(TenantSettings.class));
    }

    @Test
    void testUpdateSettingsForTenant_Success() {
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(true);
        when(settingsRepository.findByTenantId(100L)).thenReturn(Optional.of(settings));
        when(settingsRepository.save(any(TenantSettings.class))).thenAnswer(i -> i.getArgument(0));

        TenantSettingsDto updateDto = new TenantSettingsDto();
        updateDto.setMfaEnforced(true);
        updateDto.setSsoEnforced(true);
        updateDto.setPasswordRotationDays(60);
        updateDto.setMaintenanceMode(true);
        updateDto.setRateLimitPerMinute(5000);

        TenantSettingsDto result = tenantSettingsService.updateSettingsForTenant(100L, updateDto);

        assertNotNull(result);
        assertTrue(result.isSsoEnforced());
        assertTrue(result.isMaintenanceMode());
        assertEquals(60, result.getPasswordRotationDays());
        assertEquals(5000, result.getRateLimitPerMinute());
        verify(settingsRepository).save(any(TenantSettings.class));
    }

    @Test
    void testUpdateSettingsForTenant_AccessDenied() {
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(false);
        when(tenantSecurityService.getCurrentTenantId()).thenReturn(200L);

        TenantSettingsDto updateDto = new TenantSettingsDto();

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                tenantSettingsService.updateSettingsForTenant(100L, updateDto));

        assertTrue(ex.getMessage().contains("Access denied"));
    }
}
