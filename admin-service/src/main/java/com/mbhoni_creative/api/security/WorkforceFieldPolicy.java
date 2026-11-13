package com.mbhoni_creative.api.security;

import com.mbhoni_creative.adminentity.DataType;
import com.mbhoni_creative.adminentity.FieldSection;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** The single disclosure policy for workforce field definitions and values. */
public final class WorkforceFieldPolicy {
    private WorkforceFieldPolicy() {}
    public record Seed(FieldSection section, DataType type, boolean exposed) {}
    public static final Map<String, Seed> SEEDED = Map.ofEntries(
            Map.entry("qualification_type", new Seed(FieldSection.QUALIFICATION, DataType.SELECT, true)),
            Map.entry("license_number", new Seed(FieldSection.QUALIFICATION, DataType.TEXT, false)),
            Map.entry("issuing_body", new Seed(FieldSection.QUALIFICATION, DataType.TEXT, false)),
            Map.entry("license_expiry_date", new Seed(FieldSection.QUALIFICATION, DataType.DATE, false)),
            Map.entry("clearance_reference", new Seed(FieldSection.BACKGROUND_CHECK, DataType.TEXT, false)),
            Map.entry("vetting_provider", new Seed(FieldSection.BACKGROUND_CHECK, DataType.TEXT, false)),
            Map.entry("clearance_expiry_date", new Seed(FieldSection.BACKGROUND_CHECK, DataType.DATE, false)),
            Map.entry("probation_end_date", new Seed(FieldSection.EMPLOYMENT_STATUS, DataType.DATE, false)),
            Map.entry("notice_period_days", new Seed(FieldSection.EMPLOYMENT_STATUS, DataType.NUMBER, false)),
            Map.entry("work_permit_number", new Seed(FieldSection.EMPLOYMENT_STATUS, DataType.TEXT, false)),
            Map.entry("site_location_code", new Seed(FieldSection.FIELD_ASSIGNMENT, DataType.TEXT, true)),
            Map.entry("gps_checkin_radius", new Seed(FieldSection.FIELD_ASSIGNMENT, DataType.NUMBER, false)),
            Map.entry("assigned_equipment_id", new Seed(FieldSection.FIELD_ASSIGNMENT, DataType.TEXT, true)));
    private static final Set<String> SENSITIVE = Set.of("identity", "idnumber", "nationalid", "clearance", "vetting",
            "license", "licence", "permit", "passport", "bank", "account", "salary", "compensation", "payroll", "wage",
            "tax", "medical", "health", "disciplinary", "birth", "dob", "emergency", "contact", "phone", "mobile",
            "email", "address", "residence", "residential", "personal", "document", "attachment", "gps", "latitude", "longitude");

    public static boolean exposed(String key, String label, FieldSection section, DataType type, boolean global) {
        if (key == null || key.isBlank() || label == null || label.isBlank() || section == null || type == null
                || section == FieldSection.BACKGROUND_CHECK || type == DataType.FILE || type == DataType.GPS_COORD) return false;
        String normalizedKey = normalized(key);
        String normalizedLabel = normalized(label);
        if (SENSITIVE.stream().anyMatch(p -> normalizedKey.contains(p) || normalizedLabel.contains(p))) return false;
        Seed seed = SEEDED.get(key);
        if (seed != null) return seed.exposed() && seed.section() == section && seed.type() == type;
        // Global definitions cannot be assumed to be reviewed seeds.
        return !global;
    }
    private static String normalized(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
