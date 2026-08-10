package com.mbhoni_creative.adminservice.util;

import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbhoni_creative.adminentity.DataType;
import com.mbhoni_creative.adminentity.FieldDefinition;

public class FieldValueValidator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void validateValue(FieldDefinition definition, String rawValue) {
        if (definition == null) return;

        // 1. Mandatory Server-Side Required Field Check
        if (definition.isRequired()) {
            if (rawValue == null || rawValue.trim().isEmpty()) {
                throw new IllegalArgumentException("Required field missing: '" + definition.getLabel() + "' (" + definition.getFieldKey() + ").");
            }
        }

        if (rawValue == null || rawValue.trim().isEmpty()) {
            return; // Optional empty value is valid
        }

        String trimmed = rawValue.trim();

        // 2. Data Type Coercion & Format Validation
        DataType type = definition.getDataType();
        if (type != null) {
            switch (type) {
                case NUMBER:
                    try {
                        Double.parseDouble(trimmed);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Field '" + definition.getLabel() + "' must be a valid numeric value.");
                    }
                    break;
                case DATE:
                    try {
                        LocalDate.parse(trimmed);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Field '" + definition.getLabel() + "' must be a valid ISO Date (YYYY-MM-DD).");
                    }
                    break;
                case BOOLEAN:
                    if (!trimmed.equalsIgnoreCase("true") && !trimmed.equalsIgnoreCase("false")) {
                        throw new IllegalArgumentException("Field '" + definition.getLabel() + "' must be a boolean ('true' or 'false').");
                    }
                    break;
                case SELECT:
                    if (definition.getSelectOptions() != null && !definition.getSelectOptions().isBlank()) {
                        try {
                            List<String> options = objectMapper.readValue(definition.getSelectOptions(), new TypeReference<List<String>>() {});
                            if (options != null && !options.contains(trimmed)) {
                                throw new IllegalArgumentException("Invalid selection '" + trimmed + "' for '" + definition.getLabel() + "'. Allowed options: " + options);
                            }
                        } catch (Exception e) {
                            if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
                        }
                    }
                    break;
                case GPS_COORD:
                    if (!Pattern.matches("^-?\\d+(\\.\\d+)?,\\s*-?\\d+(\\.\\d+)?$", trimmed)) {
                        throw new IllegalArgumentException("Field '" + definition.getLabel() + "' must be a valid GPS coordinate (e.g. -26.2041, 28.0473).");
                    }
                    break;
                default:
                    break;
            }
        }

        // 3. Custom Regex / Validation Rule Check
        if (definition.getValidationRule() != null && !definition.getValidationRule().isBlank()) {
            try {
                if (!Pattern.matches(definition.getValidationRule(), trimmed)) {
                    throw new IllegalArgumentException("Field '" + definition.getLabel() + "' failed validation rule expression.");
                }
            } catch (Exception e) {
                if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
            }
        }
    }
}
