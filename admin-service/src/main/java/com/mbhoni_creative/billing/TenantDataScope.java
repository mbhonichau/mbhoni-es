package com.mbhoni_creative.billing;

import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Derives owned child rows from the actual FK graph, including legacy table names. */
@Component
public class TenantDataScope {
    public record ForeignKey(String column, String parent, String parentColumn, boolean nullable) {}
    public record Table(String name, Set<String> columns, List<ForeignKey> keys) {}
    public record Scope(String predicate, int parameters) {
        public Object[] arguments(Long tenantId) { return Collections.nCopies(parameters, tenantId).toArray(); }
    }
    public record Graph(Map<String, Table> tables, Map<String, Scope> scopes, List<String> deleteOrder) {}
    private final JdbcTemplate jdbc;
    public TenantDataScope(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Graph inspect() {
        return jdbc.execute((ConnectionCallback<Graph>) connection -> {
            DatabaseMetaData metadata = connection.getMetaData();
            Map<String, Table> tables = new LinkedHashMap<>();
            String catalog = connection.getCatalog(), schema = connection.getSchema();
            try (ResultSet rs = metadata.getTables(catalog, schema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String name = identifier(rs.getString("TABLE_NAME"));
                    Map<String, Boolean> nullable = new HashMap<>(); Set<String> columns = new HashSet<>();
                    try (ResultSet cs = metadata.getColumns(catalog, schema, name, "%")) {
                        while (cs.next()) {
                            String column = identifier(cs.getString("COLUMN_NAME")); columns.add(column.toLowerCase(Locale.ROOT));
                            nullable.put(column.toLowerCase(Locale.ROOT), cs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls);
                        }
                    }
                    List<ForeignKey> keys = new ArrayList<>();
                    try (ResultSet ks = metadata.getImportedKeys(catalog, schema, name)) {
                        while (ks.next()) {
                            String column = identifier(ks.getString("FKCOLUMN_NAME"));
                            keys.add(new ForeignKey(column, identifier(ks.getString("PKTABLE_NAME")), identifier(ks.getString("PKCOLUMN_NAME")),
                                    nullable.getOrDefault(column.toLowerCase(Locale.ROOT), false)));
                        }
                    }
                    tables.put(name.toLowerCase(Locale.ROOT), new Table(name, columns, keys));
                }
            }
            if (!tables.containsKey("tenants") || !tables.containsKey("tenant_subscriptions"))
                throw new IllegalStateException("Cannot verify tenant schema; refusing purge");
            Map<String, Scope> scopes = new LinkedHashMap<>();
            for (String table : tables.keySet()) scope(table, tables, scopes, new HashSet<>());
            Set<String> remaining = new LinkedHashSet<>(scopes.keySet()); List<String> order = new ArrayList<>();
            while (!remaining.isEmpty()) {
                String leaf = remaining.stream().filter(parent -> remaining.stream().noneMatch(child -> !child.equals(parent)
                        && tables.get(child).keys().stream().anyMatch(k -> k.parent().equalsIgnoreCase(parent))))
                        .findFirst().orElseThrow(() -> new IllegalStateException("Tenant FK cycle requires explicit repair before purge"));
                order.add(leaf); remaining.remove(leaf);
            }
            return new Graph(tables, scopes, order);
        });
    }
    private Scope scope(String name, Map<String, Table> tables, Map<String, Scope> scopes, Set<String> visiting) {
        if (name.equals("tenants")) return null;
        if (scopes.containsKey(name)) return scopes.get(name);
        Table table = tables.get(name); if (table == null || !visiting.add(name)) return null;
        if (table.columns().contains("tenant_id")) {
            Scope scope = new Scope("tenant_id = ?", 1); scopes.put(name, scope); visiting.remove(name); return scope;
        }
        List<String> owned = new ArrayList<>(), compatible = new ArrayList<>(); int parameters = 0;
        for (ForeignKey key : table.keys()) {
            String parentName = key.parent().toLowerCase(Locale.ROOT);
            if (parentName.equals(name)) continue;
            Scope parent = scope(parentName, tables, scopes, visiting);
            if (parent == null) continue;
            String membership = key.column() + " IN (SELECT " + key.parentColumn() + " FROM " + key.parent() + " WHERE " + parent.predicate() + ")";
            owned.add(membership); parameters += parent.parameters();
            String global = tables.get(parentName).columns().contains("tenant_id")
                    ? " OR " + key.column() + " IN (SELECT " + key.parentColumn() + " FROM " + key.parent() + " WHERE tenant_id IS NULL)" : "";
            compatible.add("(" + membership + global + (key.nullable() ? " OR " + key.column() + " IS NULL" : "") + ")");
            parameters += parent.parameters();
        }
        visiting.remove(name);
        if (owned.isEmpty()) return null;
        Scope result = new Scope("(" + String.join(" OR ", owned) + ") AND " + String.join(" AND ", compatible), parameters);
        scopes.put(name, result); return result;
    }
    private static String identifier(String identifier) {
        if (identifier == null || !identifier.matches("[A-Za-z_][A-Za-z0-9_]*")) throw new IllegalStateException("Unsupported database identifier");
        return identifier;
    }
}
