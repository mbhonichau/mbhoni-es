package com.mbhoni_creative.admincontroller.api;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.mbhoni_creative.adminentity.DataType;
import com.mbhoni_creative.adminentity.FieldDefinition;
import com.mbhoni_creative.adminentity.FieldSection;
import com.mbhoni_creative.adminservice.FieldDefinitionService;
import com.mbhoni_creative.config.TenantSecurityService;

@ExtendWith(MockitoExtension.class)
class FieldDefinitionApiControllerTest {

    @Mock
    private FieldDefinitionService fieldDefinitionService;

    @Mock
    private TenantSecurityService tenantSecurityService;

    @InjectMocks
    private FieldDefinitionApiController controller;

    private FieldDefinition field;

    @BeforeEach
    void setUp() {
        field = new FieldDefinition();
        field.setId(10L);
        field.setSection(FieldSection.QUALIFICATION);
        field.setFieldKey("test_key");
        field.setLabel("Test Label");
        field.setDataType(DataType.TEXT);
    }

    @Test
    void testGetFieldsForSection_ReturnsActiveSchemaList() {
        given(tenantSecurityService.getCurrentTenantId()).willReturn(1L);
        given(fieldDefinitionService.getEffectiveSchema(1L, FieldSection.QUALIFICATION))
                .willReturn(List.of(field));

        ResponseEntity<List<FieldDefinition>> response = controller.getFieldsForSection(FieldSection.QUALIFICATION);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("test_key", response.getBody().get(0).getFieldKey());
    }

    @Test
    void testCreateField_DelegatesToService() {
        given(tenantSecurityService.getCurrentTenantId()).willReturn(1L);
        given(fieldDefinitionService.createField(eq(1L), any(FieldDefinition.class))).willReturn(field);

        ResponseEntity<FieldDefinition> response = controller.createField(field);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Test Label", response.getBody().getLabel());
        verify(fieldDefinitionService).createField(1L, field);
    }

    @Test
    void testUpdateField_DelegatesToService() {
        given(tenantSecurityService.getCurrentTenantId()).willReturn(1L);
        given(fieldDefinitionService.updateField(eq(1L), eq(10L), any(FieldDefinition.class))).willReturn(field);

        ResponseEntity<FieldDefinition> response = controller.updateField(10L, field);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(fieldDefinitionService).updateField(1L, 10L, field);
    }

    @Test
    void testDeactivateField_DelegatesToService() {
        given(tenantSecurityService.getCurrentTenantId()).willReturn(1L);

        ResponseEntity<Void> response = controller.deactivateField(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(fieldDefinitionService).deactivateField(1L, 10L);
    }
}
