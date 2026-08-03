package com.mbhoni_creative.adminservice;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mbhoni_creative.admindto.TenantDto;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminrepository.TenantCustomizationRepository;
import com.mbhoni_creative.adminrepository.TenantModuleRepository;
import com.mbhoni_creative.adminrepository.TenantQuotaRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.adminrepository.UserRepository;
import com.mbhoni_creative.adminservice.impl.TenantServiceImpl;
import com.mbhoni_creative.config.TenantSecurityService;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantSubscriptionRepository subscriptionRepository;

    @Mock
    private TenantQuotaRepository quotaRepository;

    @Mock
    private TenantCustomizationRepository customizationRepository;

    @Mock
    private TenantModuleRepository moduleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantSecurityService tenantSecurityService;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    @Mock
    private jakarta.persistence.Query query;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TenantServiceImpl tenantService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Acme Corp");
        tenant.setTenantCode("acme");
        tenant.setActive(true);
    }

    @Test
    void testGetAllTenants() {
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(true);
        when(tenantRepository.findAll()).thenReturn(List.of(tenant));

        List<TenantDto> dtos = tenantService.getAllTenants();

        assertNotNull(dtos);
        assertEquals(1, dtos.size());
        assertEquals("Acme Corp", dtos.get(0).getName());
    }

    @Test
    void testGetTenantById_Found() {
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(true);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));

        TenantDto dto = tenantService.getTenantById(1L);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Acme Corp", dto.getName());
    }

    @Test
    void testGetTenantById_NotFound_ThrowsException() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> tenantService.getTenantById(99L));
        assertEquals("Tenant not found", ex.getMessage());
    }

    @Test
    void testSaveTenant_Success() {
        TenantDto dto = new TenantDto();
        dto.setName("New Corp");

        when(tenantSecurityService.isGlobalAdmin()).thenReturn(true);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant saved = invocation.getArgument(0);
            saved.setId(2L);
            return saved;
        });

        TenantDto result = tenantService.saveTenant(dto);

        assertNotNull(result);
        assertEquals("New Corp", result.getName());
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    void testDeleteTenant_Success() {
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(true);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(entityManager.createQuery(any(String.class))).thenReturn(query);
        when(entityManager.createNativeQuery(any(String.class))).thenReturn(query);
        when(query.setParameter(any(String.class), any())).thenReturn(query);

        tenantService.deleteTenant(1L);

        verify(tenantRepository).delete(tenant);
    }
}
