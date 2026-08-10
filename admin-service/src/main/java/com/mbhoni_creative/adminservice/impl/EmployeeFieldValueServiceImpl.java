package com.mbhoni_creative.adminservice.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.adminentity.Employee;
import com.mbhoni_creative.adminentity.EmployeeFieldValue;
import com.mbhoni_creative.adminentity.FieldDefinition;
import com.mbhoni_creative.adminrepository.EmployeeFieldValueRepository;
import com.mbhoni_creative.adminrepository.EmployeeRepository;
import com.mbhoni_creative.adminservice.EmployeeFieldValueService;
import com.mbhoni_creative.adminservice.FieldDefinitionService;
import com.mbhoni_creative.adminservice.util.FieldValueValidator;

@Service
@Transactional
public class EmployeeFieldValueServiceImpl implements EmployeeFieldValueService {

    private final EmployeeFieldValueRepository fieldValueRepository;
    private final EmployeeRepository employeeRepository;
    private final FieldDefinitionService fieldDefinitionService;

    public EmployeeFieldValueServiceImpl(
            EmployeeFieldValueRepository fieldValueRepository,
            EmployeeRepository employeeRepository,
            FieldDefinitionService fieldDefinitionService) {
        this.fieldValueRepository = fieldValueRepository;
        this.employeeRepository = employeeRepository;
        this.fieldDefinitionService = fieldDefinitionService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeFieldValue> getValuesForEmployee(Long employeeId) {
        return fieldValueRepository.findByEmployeeId(employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getValueMapForEmployee(Long employeeId) {
        List<EmployeeFieldValue> list = fieldValueRepository.findByEmployeeId(employeeId);
        Map<String, String> map = new HashMap<>();
        for (EmployeeFieldValue val : list) {
            if (val.getFieldDefinition() != null && val.getFieldDefinition().getFieldKey() != null) {
                map.put(val.getFieldDefinition().getFieldKey(), val.getValue());
            }
        }
        return map;
    }

    @Override
    public void saveValues(Long employeeId, Map<String, String> fieldValues) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found ID: " + employeeId));

        Long tenantId = employee.getTenant() != null ? employee.getTenant().getId() : null;

        // Fetch server-side effective schema across all sections
        List<FieldDefinition> effectiveSchema = fieldDefinitionService.getAllEffectiveSchemaForTenant(tenantId);

        // Mandatory Server-Side Required Field & Type Validation (Never trust client-submitted "required" state)
        for (FieldDefinition def : effectiveSchema) {
            String rawVal = fieldValues != null ? fieldValues.get(def.getFieldKey()) : null;
            
            // Validate using shared FieldValueValidator utility
            FieldValueValidator.validateValue(def, rawVal);

            if (rawVal != null && !rawVal.trim().isEmpty()) {
                Optional<EmployeeFieldValue> existingValOpt = fieldValueRepository.findByEmployeeIdAndFieldDefinitionId(employeeId, def.getId());
                EmployeeFieldValue fieldValue = existingValOpt.orElseGet(() -> {
                    EmployeeFieldValue val = new EmployeeFieldValue();
                    val.setEmployee(employee);
                    val.setFieldDefinition(def);
                    return val;
                });
                fieldValue.setValue(rawVal.trim());
                fieldValue.setRecordedAt(LocalDateTime.now());
                fieldValueRepository.save(fieldValue);
            }
        }
    }
}
