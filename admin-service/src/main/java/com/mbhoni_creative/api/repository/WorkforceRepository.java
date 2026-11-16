package com.mbhoni_creative.api.repository;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.api.dto.WorkforceDtos.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Repository;

/** Fixed SQL projections avoid entity graph loading and OSIV dependencies. */
@Repository
public class WorkforceRepository {
    private final NamedParameterJdbcTemplate jdbc;
    public WorkforceRepository(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Unit(Long id, String name, String code, Long parent, String type, boolean isolated, LocalDateTime updatedAt) {}
    public record Definition(Long id, Long tenant, String key, String label, FieldSection section, DataType type,
            boolean required, boolean active, int order, String options) {}
    public record Value(Long employee, Long definition, String value) {}
    public record EmployeeRow(Long id, String number, String first, String last, String title, String status, String employmentType,
            Long org, Long manager, LocalDate start, LocalDateTime updatedAt) {}
    private static Long id(ResultSet r, String column) throws SQLException { return r.getObject(column, Long.class); }
    private static LocalDateTime time(ResultSet r, String column) throws SQLException {
        return r.getObject(column, LocalDateTime.class);
    }
    public List<Unit> units(long tenant) {
        return jdbc.query("SELECT id,name,unit_code,parent_unit_id,unit_type,isolated,updated_at FROM organization_units WHERE tenant_id=:tenant ORDER BY id",
                Map.of("tenant",tenant), (r,n) -> new Unit(id(r,"id"), r.getString("name"), r.getString("unit_code"), id(r,"parent_unit_id"),
                        r.getString("unit_type"), r.getBoolean("isolated"), time(r,"updated_at")));
    }
    public List<Definition> definitions(long tenant) {
        return jdbc.query("SELECT id,tenant_id,field_key,label,section,data_type,is_required,is_active,display_order,select_options FROM field_definitions "
                + "WHERE tenant_id=:tenant OR tenant_id IS NULL ORDER BY id", Map.of("tenant",tenant),
                (r,n) -> new Definition(id(r,"id"),id(r,"tenant_id"),r.getString("field_key"),r.getString("label"),
                        FieldSection.valueOf(r.getString("section")),DataType.valueOf(r.getString("data_type")),r.getBoolean("is_required"),
                        r.getBoolean("is_active"),r.getInt("display_order"),r.getString("select_options")));
    }
    private static String visible(String alias, Set<Long> allowed) {
        return "("+alias+".organization_unit_id IS NULL" + (allowed.isEmpty() ? "" : " OR "+alias+".organization_unit_id IN (:allowed)") + ")";
    }
    private static final String VALUE_SCOPE = " FROM employee_field_values v JOIN field_definitions d ON d.id=v.field_definition_id "
            + "WHERE v.employee_id=e.id AND (d.tenant_id=:tenant OR d.tenant_id IS NULL)";
    private static String where(Set<Long> allowed, Long employee, Long org, String status, LocalDateTime since) {
        return " WHERE e.tenant_id=:tenant AND " + visible("e",allowed)
                + (employee == null ? "" : " AND e.id=:employee") + (org == null ? "" : " AND e.organization_unit_id=:org")
                + (status == null ? "" : " AND e.status=:status")
                + (since == null ? "" : " AND (e.updated_at>=:since OR EXISTS (SELECT 1"+VALUE_SCOPE+" AND v.recorded_at>=:since))");
    }
    private static MapSqlParameterSource params(long tenant, Set<Long> allowed, Long employee, Long org, String status, LocalDateTime since) {
        return new MapSqlParameterSource("tenant",tenant).addValue("allowed",allowed).addValue("employee",employee)
                .addValue("org",org).addValue("status",status).addValue("since",since);
    }
    public long count(long tenant, Set<Long> allowed, Long org, String status, LocalDateTime since) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM employees e"+where(allowed,null,org,status,since),
                params(tenant,allowed,null,org,status,since),Long.class);
    }
    public List<EmployeeRow> employees(long tenant, Set<Long> allowed, Long employee, Long org, String status, LocalDateTime since, int page, int size) {
        String sql = "SELECT e.id,e.employee_number,e.first_name,e.last_name,e.job_title,e.status,e.employment_type,e.organization_unit_id,"
                + "CASE WHEN m.tenant_id=:tenant AND "+visible("m",allowed)+" THEN m.id ELSE NULL END AS visible_manager,e.hire_date,e.updated_at,"
                + "(SELECT MAX(v.recorded_at)"+VALUE_SCOPE+") AS value_updated_at FROM employees e LEFT JOIN employees m ON m.id=e.manager_employee_id"
                + where(allowed,employee,org,status,since)+" ORDER BY e.id LIMIT :limit OFFSET :offset";
        var p=params(tenant,allowed,employee,org,status,since).addValue("limit",size).addValue("offset",(long)page*size);
        return jdbc.query(sql,p,(r,n) -> {
            LocalDateTime own=time(r,"updated_at"), values=time(r,"value_updated_at");
            LocalDateTime latest=own == null ? values : values == null || own.isAfter(values) ? own : values;
            java.sql.Date start=r.getDate("hire_date");
            return new EmployeeRow(id(r,"id"),r.getString("employee_number"),r.getString("first_name"),r.getString("last_name"),
                    r.getString("job_title"),r.getString("status"),r.getString("employment_type"),id(r,"organization_unit_id"),
                    id(r,"visible_manager"),start == null ? null : start.toLocalDate(),latest);
        });
    }
    public List<Value> values(long tenant, List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return jdbc.query("SELECT v.employee_id,v.field_definition_id,v.value FROM employee_field_values v "
                + "JOIN employees e ON e.id=v.employee_id JOIN field_definitions d ON d.id=v.field_definition_id "
                + "WHERE e.tenant_id=:tenant AND (d.tenant_id=:tenant OR d.tenant_id IS NULL) AND e.id IN (:ids) ORDER BY v.id",
                new MapSqlParameterSource("tenant",tenant).addValue("ids",ids),
                (r,n) -> new Value(id(r,"employee_id"),id(r,"field_definition_id"),r.getString("value")));
    }
    public List<Reference> references(long tenant, String category) {
        // Tenant entries (including inactive tombstones) override matching global codes.
        return jdbc.query("SELECT l.id,l.code,l.value,l.locale FROM lookup_codes l JOIN lookup_categories c ON c.id=l.category_id "
                + "WHERE c.code=:category AND (c.tenant_id=:tenant OR c.tenant_id IS NULL) AND l.active=true "
                + "AND (c.tenant_id=:tenant OR NOT EXISTS (SELECT 1 FROM lookup_codes o JOIN lookup_categories oc ON oc.id=o.category_id "
                + "WHERE oc.tenant_id=:tenant AND oc.code=c.code AND o.code=l.code)) ORDER BY l.id",
                Map.of("tenant",tenant,"category",category),(r,n) -> new Reference(id(r,"id"),r.getString("code"),r.getString("value"),r.getString("locale")));
    }
}
