package com.mbhoni_creative.api;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.api.security.WorkforceFieldPolicy;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class WorkforceFieldPolicyTest {
    @Test void everySeedHasAnExplicitDecisionMatchingTheMigration() throws Exception {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V5__add_field_definitions_and_employee_values.sql"));
        var rows = Pattern.compile("\\(NULL, '([^']+)', '([^']+)', '([^']+)', '([^']+)'").matcher(sql);
        Set<String> seen = new HashSet<>();
        Set<String> safe = Set.of("qualification_type", "site_location_code", "assigned_equipment_id");
        while (rows.find()) {
            String key = rows.group(2); seen.add(key);
            var decision = WorkforceFieldPolicy.SEEDED.get(key); assertNotNull(decision, key);
            assertEquals(FieldSection.valueOf(rows.group(1)), decision.section());
            assertEquals(DataType.valueOf(rows.group(4)), decision.type());
            for (boolean global : new boolean[]{true, false})
                assertEquals(safe.contains(key), WorkforceFieldPolicy.exposed(key, rows.group(3), decision.section(), decision.type(), global), key);
        }
        assertEquals(WorkforceFieldPolicy.SEEDED.keySet(), seen);
    }
    @Test void tenantDefinitionsFailClosedForSensitiveNamesSectionsAndTypes() {
        for (String text : List.of("National ID", "id_number", "passport", "bank", "account", "salary", "tax", "medical",
                "disciplinary", "birth", "emergency", "clearance", "vetting", "licence", "license", "permit", "personal email",
                "phone", "home address", "Date Of Birth", "DOB", "attachment", "gps", "compensation")) {
            assertFalse(WorkforceFieldPolicy.exposed(text, "Ordinary label", FieldSection.FIELD_ASSIGNMENT, DataType.TEXT, false), text);
            assertFalse(WorkforceFieldPolicy.exposed("ordinary_key", text, FieldSection.FIELD_ASSIGNMENT, DataType.TEXT, false), text);
        }
        for (DataType type : List.of(DataType.FILE, DataType.GPS_COORD))
            assertFalse(WorkforceFieldPolicy.exposed("safe", "Safe", FieldSection.FIELD_ASSIGNMENT, type, false));
        assertFalse(WorkforceFieldPolicy.exposed("result", "Result", FieldSection.BACKGROUND_CHECK, DataType.TEXT, false));
        assertFalse(WorkforceFieldPolicy.exposed("new_global", "New global", FieldSection.FIELD_ASSIGNMENT, DataType.TEXT, true));
        assertFalse(WorkforceFieldPolicy.exposed("qualification_type", "Qualification", FieldSection.FIELD_ASSIGNMENT, DataType.TEXT, false));
        assertFalse(WorkforceFieldPolicy.exposed(null, "Safe", FieldSection.FIELD_ASSIGNMENT, DataType.TEXT, false));
        assertTrue(WorkforceFieldPolicy.exposed("shirt_size", "Shirt size", FieldSection.FIELD_ASSIGNMENT, DataType.TEXT, false));
        assertTrue(WorkforceFieldPolicy.exposed("cost_centre", "Cost centre", FieldSection.FIELD_ASSIGNMENT, DataType.TEXT, false));
    }
}
