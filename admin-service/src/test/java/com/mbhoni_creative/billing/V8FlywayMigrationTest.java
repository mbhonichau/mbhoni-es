package com.mbhoni_creative.billing;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Opt in only with the disposable MySQL server used by Database/tests/V8_scope_regression.ps1. */
@EnabledIfEnvironmentVariable(named = "V8_MYSQL_INTEGRATION", matches = "true")
class V8FlywayMigrationTest {
    @TempDir Path migrations;
    @Test void flywayResumesAnExistingScopeColumnAndValidatesCompletedMigration() throws Exception {
        String server = "jdbc:mysql://127.0.0.1:13368/";
        String schema = "v8_flyway_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(server, "root", ""); Statement sql = connection.createStatement()) {
            try (ResultSet rows = sql.executeQuery("SELECT @@datadir")) {
                assertTrue(rows.next());
                assertEquals(Path.of("target/v8-mysql-regression").toAbsolutePath().normalize(), Path.of(rows.getString(1)).toAbsolutePath().normalize(),
                        "Refusing to mutate anything except the disposable database server");
            }
            sql.execute("CREATE DATABASE " + schema);
            try {
                sql.execute("USE " + schema);
                sql.execute("CREATE TABLE tenants(id BIGINT PRIMARY KEY) ENGINE=InnoDB");
                sql.execute("CREATE TABLE field_definitions(id BIGINT PRIMARY KEY, tenant_id BIGINT, section VARCHAR(40), field_key VARCHAR(80), FOREIGN KEY(tenant_id) REFERENCES tenants(id) ON DELETE CASCADE) ENGINE=InnoDB");
                sql.execute("CREATE TABLE lookup_categories(id BIGINT PRIMARY KEY, tenant_id BIGINT, code VARCHAR(80) UNIQUE, FOREIGN KEY(tenant_id) REFERENCES tenants(id) ON DELETE CASCADE) ENGINE=InnoDB");
                sql.execute("ALTER TABLE field_definitions ADD tenant_scope BIGINT GENERATED ALWAYS AS (COALESCE(tenant_id,0)) VIRTUAL");
                Path source = Path.of("src/main/resources/db/migration/V8__scope_shared_defaults.sql");
                Files.copy(source, migrations.resolve(source.getFileName()));
                Flyway flyway = Flyway.configure().dataSource(server + schema, "root", "")
                        .locations("filesystem:" + migrations.toAbsolutePath()).baselineOnMigrate(true).baselineVersion("7").load();
                assertEquals(1, flyway.migrate().migrationsExecuted);
                flyway.validate();
                assertEquals(0, flyway.migrate().migrationsExecuted);
                try (ResultSet rows = sql.executeQuery("SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema = '" + schema + "' AND routine_name = 'v8_migrate_shared_default_scope'")) {
                    assertTrue(rows.next()); assertEquals(0, rows.getInt(1), "Successful migration removes its helper");
                }
            } finally {
                sql.execute("DROP DATABASE " + schema);
            }
        }
    }
}
