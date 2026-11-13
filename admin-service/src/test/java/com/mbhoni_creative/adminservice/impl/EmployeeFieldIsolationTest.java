package com.mbhoni_creative.adminservice.impl;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.FieldDefinitionService;
import com.mbhoni_creative.config.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmployeeFieldIsolationTest {
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final EmployeeFieldValueRepository values = mock(EmployeeFieldValueRepository.class);
    private final FieldDefinitionService schema = mock(FieldDefinitionService.class);
    private final EmployeeFieldValueServiceImpl service = new EmployeeFieldValueServiceImpl(values,
            employees, schema, new TenantAccessService(new TenantSecurityService()), mock(OrganizationAccessService.class));
    private Employee employee;
    @BeforeEach void setup() {
        Tenant own = new Tenant(); own.setId(1L);
        User user = new User(); user.setTenant(own);
        CustomUserPrincipal principal = new CustomUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        Tenant foreign = new Tenant(); foreign.setId(2L);
        employee = new Employee(); employee.setTenant(foreign);
        when(employees.findById(20L)).thenReturn(Optional.of(employee));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void foreignValuesCannotBeRead() {
        assertThrows(AccessDeniedException.class, () -> service.getValueMapForEmployee(20L));
        assertThrows(AccessDeniedException.class, () -> service.getValuesForEmployee(20L));
        verifyNoInteractions(values, schema);
    }
    @Test void foreignValuesCannotBeWritten() {
        assertThrows(AccessDeniedException.class, () -> service.saveValues(20L, Map.of("license", "changed")));
        verifyNoInteractions(values, schema);
    }
    @Test void ownValuesRemainReadable() {
        employee.getTenant().setId(1L);
        when(values.findByEmployeeId(20L)).thenReturn(List.of());
        assertEquals(Map.of(), service.getValueMapForEmployee(20L));
        verify(values).findByEmployeeId(20L);
    }
}
