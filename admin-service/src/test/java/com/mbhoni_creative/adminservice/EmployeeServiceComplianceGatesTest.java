package com.mbhoni_creative.adminservice;

import java.time.LocalDate;
import java.util.List;
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

import com.mbhoni_creative.adminentity.BackgroundCheckStatus;
import com.mbhoni_creative.adminentity.ComplianceEmploymentStatus;
import com.mbhoni_creative.adminentity.DataType;
import com.mbhoni_creative.adminentity.Employee;
import com.mbhoni_creative.adminentity.EmployeeFieldValue;
import com.mbhoni_creative.adminentity.EmployeeStatus;
import com.mbhoni_creative.adminentity.FieldDefinition;
import com.mbhoni_creative.adminentity.FieldSection;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminrepository.EmployeeFieldValueRepository;
import com.mbhoni_creative.adminrepository.EmployeeRepository;
import com.mbhoni_creative.adminrepository.OrganizationUnitRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.impl.EmployeeServiceImpl;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceComplianceGatesTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private OrganizationUnitRepository orgUnitRepository;

    @Mock
    private FieldDefinitionService fieldDefinitionService;

    @Mock
    private EmployeeFieldValueRepository fieldValueRepository;

    @Mock
    private com.mbhoni_creative.adminrepository.PayslipRepository payslipRepository;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private Tenant tenant;
    private Employee employee;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(1L);

        employee = new Employee();
        employee.setId(10L);
        employee.setTenant(tenant);
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john.doe@example.com");
        employee.setEmployeeNumber("EMP-1001");
        employee.setComplianceEmploymentStatus(ComplianceEmploymentStatus.ACTIVE);
        employee.setBackgroundCheckStatus(BackgroundCheckStatus.NOT_STARTED);
    }

    @Test
    void testSaveEmployee_HardcodedGate_BlocksActiveStatusWhenBackgroundCheckNotPassed() {
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));
        // Attempting to save ACTIVE status while background check is NOT_STARTED
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                employeeService.saveEmployee(1L, null, null, employee));

        assertTrue(ex.getMessage().contains("Compliance State Machine Gate"));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void testSaveEmployee_HardcodedGate_AllowsActiveStatusWhenBackgroundCheckPassed() {
        employee.setId(null); // New employee creation
        employee.setBackgroundCheckStatus(BackgroundCheckStatus.PASSED);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(employeeRepository.save(any(Employee.class))).thenReturn(employee);

        Employee saved = employeeService.saveEmployee(1L, null, null, employee);

        assertNotNull(saved);
        assertEquals(BackgroundCheckStatus.PASSED, saved.getBackgroundCheckStatus());
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    void testValidateFieldAssignmentEligibility_BlocksNonActiveStatus() {
        employee.setStatus(EmployeeStatus.SUSPENDED);
        employee.setComplianceEmploymentStatus(ComplianceEmploymentStatus.SUSPENDED);
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                employeeService.validateFieldAssignmentEligibility(10L));

        assertTrue(ex.getMessage().contains("Field Assignment Gate Violation: Employee status must be ACTIVE"));
    }

    @Test
    void testValidateFieldAssignmentEligibility_BlocksWhenRequiredExpiryDateIsPastDue() {
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setComplianceEmploymentStatus(ComplianceEmploymentStatus.ACTIVE);
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));

        FieldDefinition dateDef = new FieldDefinition();
        dateDef.setId(100L);
        dateDef.setSection(FieldSection.QUALIFICATION);
        dateDef.setFieldKey("license_expiry_date");
        dateDef.setLabel("License Expiration Date");
        dateDef.setDataType(DataType.DATE);
        dateDef.setRequired(true);

        when(fieldDefinitionService.getAllEffectiveSchemaForTenant(1L)).thenReturn(List.of(dateDef));

        EmployeeFieldValue expiredValue = new EmployeeFieldValue();
        expiredValue.setEmployee(employee);
        expiredValue.setFieldDefinition(dateDef);
        expiredValue.setValue(LocalDate.now().minusDays(5).toString()); // Expired 5 days ago

        when(fieldValueRepository.findByEmployeeId(10L)).thenReturn(List.of(expiredValue));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                employeeService.validateFieldAssignmentEligibility(10L));

        assertTrue(ex.getMessage().contains("is expired"));
    }

    @Test
    void testValidateFieldAssignmentEligibility_PassesWhenStatusIsActiveAndNoExpiredFields() {
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setComplianceEmploymentStatus(ComplianceEmploymentStatus.ACTIVE);
        when(employeeRepository.findById(10L)).thenReturn(Optional.of(employee));

        FieldDefinition dateDef = new FieldDefinition();
        dateDef.setId(100L);
        dateDef.setSection(FieldSection.QUALIFICATION);
        dateDef.setFieldKey("license_expiry_date");
        dateDef.setLabel("License Expiration Date");
        dateDef.setDataType(DataType.DATE);
        dateDef.setRequired(true);

        when(fieldDefinitionService.getAllEffectiveSchemaForTenant(1L)).thenReturn(List.of(dateDef));

        EmployeeFieldValue validValue = new EmployeeFieldValue();
        validValue.setEmployee(employee);
        validValue.setFieldDefinition(dateDef);
        validValue.setValue(LocalDate.now().plusMonths(6).toString()); // Valid for 6 more months

        when(fieldValueRepository.findByEmployeeId(10L)).thenReturn(List.of(validValue));

        assertDoesNotThrow(() -> employeeService.validateFieldAssignmentEligibility(10L));
    }
}
