package com.mbhoni_creative.adminservice.impl;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.FieldDefinitionService;
import com.mbhoni_creative.config.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeIsolationTest {
    private final EmployeeRepository repository = mock(EmployeeRepository.class);
    private final TenantRepository tenants = mock(TenantRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final EmployeeServiceImpl service = new EmployeeServiceImpl(repository, tenants,
            mock(OrganizationUnitRepository.class), mock(FieldDefinitionService.class),
            mock(EmployeeFieldValueRepository.class), mock(PayslipRepository.class),
            new TenantAccessService(new TenantSecurityService()), mock(OrganizationAccessService.class), users);
    @BeforeEach void setup() {
        Tenant tenant = new Tenant(); tenant.setId(1L);
        when(tenants.lockById(1L)).thenReturn(Optional.of(tenant));
        User user = new User(); user.setTenant(tenant);
        CustomUserPrincipal principal = new CustomUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void createWithForeignIdCannotMutateEmployee() {
        Tenant tenant = new Tenant(); tenant.setId(2L);
        Employee existing = new Employee(); existing.setId(20L); existing.setTenant(tenant);
        existing.setFirstName("Original");
        when(repository.findById(20L)).thenReturn(Optional.of(existing));
        Employee input = new Employee(); input.setId(20L); input.setFirstName("Changed");
        input.setLastName("Name"); input.setEmail("test@example.com");
        assertThrows(AccessDeniedException.class, () -> service.saveEmployee(1L, null, null, input));
        assertEquals("Original", existing.getFirstName());
        verify(repository, never()).save(any());
    }
    @Test void nullTenantCannotListAllEmployees() {
        SecurityContextHolder.clearContext();
        assertThrows(AccessDeniedException.class, () -> service.getEmployeesByTenant(null));
        verifyNoInteractions(repository);
    }
    @Test void createCannotLinkForeignUserEvenWhenPayloadClaimsOwnTenant() {
        Employee input = new Employee(); input.setFirstName("A"); input.setLastName("B"); input.setEmail("a@example.com");
        User forged = new User(); forged.setId(99L); forged.setTenant(ContractIsolationTest.tenant(1L)); input.setUser(forged);
        when(users.findByIdAndTenantId(99L, 1L)).thenReturn(Optional.empty());
        assertThrows(AccessDeniedException.class, () -> service.saveEmployee(1L, null, null, input));
        verify(repository, never()).save(any());
    }
    @Test void ownTenantListUsesTenantQuery() {
        service.getEmployeesByTenant(null);
        verify(repository).findByTenantId(1L);
        verify(repository, never()).findAll();
    }
}
