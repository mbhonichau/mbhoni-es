package com.mbhoni_creative.adminservice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import com.mbhoni_creative.adminentity.DataType;
import com.mbhoni_creative.adminentity.Employee;
import com.mbhoni_creative.adminentity.FieldDefinition;
import com.mbhoni_creative.adminentity.FieldSection;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminrepository.EmployeeFieldValueRepository;
import com.mbhoni_creative.adminrepository.EmployeeRepository;
import com.mbhoni_creative.adminservice.impl.EmployeeFieldValueServiceImpl;

@ExtendWith(MockitoExtension.class)
class EmployeeFieldValueServiceTest {

    @Mock
    private EmployeeFieldValueRepository fieldValueRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private FieldDefinitionService fieldDefinitionService;

    @InjectMocks
    private EmployeeFieldValueServiceImpl fieldValueService;

    private Tenant tenant;
    private Employee employee;
    private FieldDefinition requiredLicenseField;
    private FieldDefinition numberField;
    private FieldDefinition dateField;
    private FieldDefinition selectField;
    private FieldDefinition gpsField;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(100L);

        employee = new Employee();
        employee.setId(500L);
        employee.setTenant(tenant);

        requiredLicenseField = new FieldDefinition();
        requiredLicenseField.setId(1L);
        requiredLicenseField.setSection(FieldSection.QUALIFICATION);
        requiredLicenseField.setFieldKey("license_number");
        requiredLicenseField.setLabel("License Number");
        requiredLicenseField.setDataType(DataType.TEXT);
        requiredLicenseField.setRequired(true);

        numberField = new FieldDefinition();
        numberField.setId(2L);
        numberField.setSection(FieldSection.FIELD_ASSIGNMENT);
        numberField.setFieldKey("gps_checkin_radius");
        numberField.setLabel("GPS Radius");
        numberField.setDataType(DataType.NUMBER);
        numberField.setRequired(false);

        dateField = new FieldDefinition();
        dateField.setId(3L);
        dateField.setSection(FieldSection.QUALIFICATION);
        dateField.setFieldKey("license_expiry_date");
        dateField.setLabel("License Expiration Date");
        dateField.setDataType(DataType.DATE);
        dateField.setRequired(false);

        selectField = new FieldDefinition();
        selectField.setId(4L);
        selectField.setSection(FieldSection.QUALIFICATION);
        selectField.setFieldKey("qualification_type");
        selectField.setLabel("Qualification Type");
        selectField.setDataType(DataType.SELECT);
        selectField.setSelectOptions("[\"BACHELORS\",\"MASTERS\",\"DIPLOMA\"]");
        selectField.setRequired(false);

        gpsField = new FieldDefinition();
        gpsField.setId(5L);
        gpsField.setSection(FieldSection.FIELD_ASSIGNMENT);
        gpsField.setFieldKey("site_gps_coords");
        gpsField.setLabel("Site GPS Coordinates");
        gpsField.setDataType(DataType.GPS_COORD);
        gpsField.setRequired(false);
    }

    @Test
    void testSaveValues_ServerSideRequiredFieldEnforcement_ThrowsWhenMissing() {
        when(employeeRepository.findById(500L)).thenReturn(Optional.of(employee));
        when(fieldDefinitionService.getAllEffectiveSchemaForTenant(100L))
                .thenReturn(List.of(requiredLicenseField));

        Map<String, String> payload = new HashMap<>(); // Empty payload — client omitted required field

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                fieldValueService.saveValues(500L, payload));

        assertTrue(ex.getMessage().contains("Required field missing"));
        verify(fieldValueRepository, never()).save(any());
    }

    @Test
    void testSaveValues_TypeValidation_InvalidNumber_ThrowsException() {
        when(employeeRepository.findById(500L)).thenReturn(Optional.of(employee));
        when(fieldDefinitionService.getAllEffectiveSchemaForTenant(100L))
                .thenReturn(List.of(numberField));

        Map<String, String> payload = Map.of("gps_checkin_radius", "not-a-number");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                fieldValueService.saveValues(500L, payload));

        assertTrue(ex.getMessage().contains("must be a valid numeric value"));
    }

    @Test
    void testSaveValues_TypeValidation_InvalidDate_ThrowsException() {
        when(employeeRepository.findById(500L)).thenReturn(Optional.of(employee));
        when(fieldDefinitionService.getAllEffectiveSchemaForTenant(100L))
                .thenReturn(List.of(dateField));

        Map<String, String> payload = Map.of("license_expiry_date", "2026/13/45");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                fieldValueService.saveValues(500L, payload));

        assertTrue(ex.getMessage().contains("valid ISO Date"));
    }

    @Test
    void testSaveValues_TypeValidation_InvalidSelectOption_ThrowsException() {
        when(employeeRepository.findById(500L)).thenReturn(Optional.of(employee));
        when(fieldDefinitionService.getAllEffectiveSchemaForTenant(100L))
                .thenReturn(List.of(selectField));

        Map<String, String> payload = Map.of("qualification_type", "INVALID_DEGREE");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                fieldValueService.saveValues(500L, payload));

        assertTrue(ex.getMessage().contains("Invalid selection"));
    }

    @Test
    void testSaveValues_TypeValidation_InvalidGpsCoord_ThrowsException() {
        when(employeeRepository.findById(500L)).thenReturn(Optional.of(employee));
        when(fieldDefinitionService.getAllEffectiveSchemaForTenant(100L))
                .thenReturn(List.of(gpsField));

        Map<String, String> payload = Map.of("site_gps_coords", "invalid_gps");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                fieldValueService.saveValues(500L, payload));

        assertTrue(ex.getMessage().contains("valid GPS coordinate"));
    }

    @Test
    void testSaveValues_ValidPayload_SavesSuccessfully() {
        when(employeeRepository.findById(500L)).thenReturn(Optional.of(employee));
        when(fieldDefinitionService.getAllEffectiveSchemaForTenant(100L))
                .thenReturn(List.of(requiredLicenseField, numberField, selectField));
        when(fieldValueRepository.findByEmployeeIdAndFieldDefinitionId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        Map<String, String> payload = Map.of(
                "license_number", "LIC-998877",
                "gps_checkin_radius", "500",
                "qualification_type", "BACHELORS"
        );

        fieldValueService.saveValues(500L, payload);

        verify(fieldValueRepository, times(3)).save(any());
    }
}
