package com.mbhoni_creative.billing;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Runs only against the disposable local MySQL migration-test server. */
@EnabledIfEnvironmentVariable(named = "V12_MYSQL_INTEGRATION", matches = "true")
class V12FlywayMigrationTest {
    @TempDir Path migrations;

    @Test void migratesBaselineInvoicesWithoutReplacingCurrencyOrLosingData() throws Exception {
        String server = "jdbc:mysql://127.0.0.1:13369/";
        String schema = "v12_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = DriverManager.getConnection(server, "root", "");
             var sql = connection.createStatement()) {
            try (var rows = sql.executeQuery("SELECT @@datadir")) {
                assertTrue(rows.next());
                assertEquals(Path.of("target/v12-mysql-regression").toAbsolutePath().normalize(),
                        Path.of(rows.getString(1)).toAbsolutePath().normalize(),
                        "Refusing to modify a non-test database server");
            }
            sql.execute("CREATE DATABASE " + schema);
            try {
                sql.execute("USE " + schema);
                sql.execute("CREATE TABLE tenants(id BIGINT PRIMARY KEY) ENGINE=InnoDB");
                String baseline = Files.readString(Path.of("src/main/resources/db/migration/V1__baseline_schema.sql"));
                int start = baseline.indexOf("CREATE TABLE IF NOT EXISTS `invoices`");
                assertTrue(start >= 0);
                sql.execute(baseline.substring(start, baseline.indexOf(';', start) + 1));
                sql.execute("INSERT INTO tenants VALUES(1)");
                sql.execute("INSERT INTO invoices(id,tenant_id,invoice_number,amount,total_amount,currency,issue_date,due_date) "
                        + "VALUES(1,1,'legacy-zar',12.34,12.34,'ZAR',CURRENT_DATE,CURRENT_DATE),"
                        + "(2,1,'legacy-usd',56.78,56.78,'USD',CURRENT_DATE,CURRENT_DATE)");
                Path source = Path.of("src/main/resources/db/migration/V12__stripe_invoice_snapshot.sql");
                Files.copy(source, migrations.resolve(source.getFileName()));
                Flyway flyway = Flyway.configure().dataSource(server + schema, "root", "")
                        .locations("filesystem:" + migrations.toAbsolutePath())
                        .baselineOnMigrate(true).baselineVersion("11").load();
                assertEquals(1, flyway.migrate().migrationsExecuted);
                flyway.validate();
                assertEquals(0, flyway.migrate().migrationsExecuted);
                try (var rows = sql.executeQuery("SELECT currency,total_amount,stripe_invoice_id,provider_amount_remaining,"
                        + "hosted_invoice_url,stripe_invoice_json FROM invoices ORDER BY id")) {
                    for (String[] expected : new String[][]{{"ZAR", "12.34"}, {"USD", "56.78"}}) {
                        assertTrue(rows.next());
                        assertEquals(expected[0], rows.getString(1));
                        assertEquals(expected[1], rows.getBigDecimal(2).toPlainString());
                        for (int column = 3; column <= 6; column++) assertNull(rows.getObject(column));
                    }
                    assertFalse(rows.next());
                }
                sql.execute("UPDATE invoices SET stripe_invoice_id='in_unique' WHERE id=1");
                var duplicate = assertThrows(java.sql.SQLException.class,
                        () -> sql.execute("UPDATE invoices SET stripe_invoice_id='in_unique' WHERE id=2"));
                assertEquals(1062, duplicate.getErrorCode());
            } finally {
                sql.execute("DROP DATABASE " + schema);
            }
        }
    }
}
