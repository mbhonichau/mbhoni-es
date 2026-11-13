package com.mbhoni_creative.adminservice.impl;

import com.mbhoni_creative.admincontroller.EmployeeViewController;
import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.PayslipRepository;
import com.mbhoni_creative.adminservice.*;
import com.mbhoni_creative.config.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PayslipIsolationTest {
    final PayslipRepository repository = mock(PayslipRepository.class);
    final EmployeeService employees = mock(EmployeeService.class);
    final OrganizationAccessService organizations = mock(OrganizationAccessService.class);
    final TenantCustomizationService customization = mock(TenantCustomizationService.class);
    final TenantSecurityService security = new TenantSecurityService();
    final EmployeeViewController controller = new EmployeeViewController(employees, mock(OrganizationUnitService.class),
            mock(TenantService.class), security, repository, customization, mock(PayslipExcelService.class), mock(PayslipWordService.class),
            mock(TenantOnboardingFieldService.class), mock(FieldDefinitionService.class), mock(EmployeeFieldValueService.class),
            new TenantAccessService(security), organizations);
    @BeforeEach void authenticate() {
        User user = new User(); user.setTenant(ContractIsolationTest.tenant(1L));
        var principal = new CustomUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    Payslip payslip(Long tenantId) {
        Employee employee = new Employee(); employee.setId(10L); employee.setTenant(ContractIsolationTest.tenant(tenantId));
        Payslip payslip = new Payslip(); payslip.setTenant(employee.getTenant()); payslip.setEmployee(employee); return payslip;
    }
    @Test void foreignPrintIsRejectedBeforeLoadingTemplates() {
        when(repository.findById(20L)).thenReturn(Optional.of(payslip(2L)));
        assertThrows(AccessDeniedException.class, () -> controller.printPayslip(20L, new ExtendedModelMap()));
        verifyNoInteractions(customization);
    }
    @Test void foreignIdCannotBeOverwrittenByGenerate() {
        when(repository.findById(20L)).thenReturn(Optional.of(payslip(2L)));
        when(employees.getEmployeeById(10L)).thenReturn(payslip(1L).getEmployee());
        Payslip payload = payslip(1L); ReflectionTestUtils.setField(payload, "id", 20L);
        assertThrows(AccessDeniedException.class, () -> controller.generatePayslip(payload, 10L, new RedirectAttributesModelMap()));
        verify(repository, never()).save(any());
    }
    @Test void isolatedEmployeeIsExcludedFromListAndCannotBePrinted() {
        Payslip hidden = payslip(1L); OrganizationUnit unit = new OrganizationUnit(); hidden.getEmployee().setOrganizationUnit(unit);
        when(repository.findByTenantIdOrderByPayDateDesc(1L)).thenReturn(List.of(hidden));
        when(organizations.canAccess(unit)).thenReturn(false);
        var model = new ExtendedModelMap(); controller.listPayslips(model);
        assertEquals(List.of(), model.get("payslips"));
        when(repository.findById(20L)).thenReturn(Optional.of(hidden));
        doThrow(new AccessDeniedException("Membership required")).when(organizations).requireAccess(unit);
        assertThrows(AccessDeniedException.class, () -> controller.printPayslip(20L, new ExtendedModelMap()));
        verifyNoInteractions(customization);
    }
}
