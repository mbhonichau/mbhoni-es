package com.mbhoni_creative.api;

import java.nio.file.*;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ApiIdempotencyMigrationTest {
    @TempDir Path migrations;
    @Test void v13WorksOnH2AndEnforcesTenantScopedCaseSensitiveKeys() throws Exception {
        String url="jdbc:h2:mem:v13_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1";
        verify(url,"sa","");
    }
    @Test @EnabledIfEnvironmentVariable(named="V8_MYSQL_INTEGRATION",matches="true")
    void v13WorksOnDisposableMySql() throws Exception {
        String server="jdbc:mysql://127.0.0.1:13368/";
        String schema="v13_test_"+UUID.randomUUID().toString().replace("-","");
        try(var c=DriverManager.getConnection(server,"root","");var sql=c.createStatement()) {
            try(var r=sql.executeQuery("SELECT @@datadir")) {
                assertTrue(r.next()); assertEquals(Path.of("target/v8-mysql-regression").toAbsolutePath().normalize(),Path.of(r.getString(1)).toAbsolutePath().normalize());
            }
            sql.execute("CREATE DATABASE "+schema);
            try { verify(server+schema,"root",""); } finally { sql.execute("DROP DATABASE "+schema); }
        }
    }
    private void verify(String url,String user,String password) throws Exception {
        try(var c=DriverManager.getConnection(url,user,password);var sql=c.createStatement()) {
            sql.execute("CREATE TABLE tenants(id BIGINT PRIMARY KEY)"); sql.execute("INSERT INTO tenants VALUES(1),(2)");
        }
        Path source=Path.of("src/main/resources/db/migration/V13__api_idempotency.sql"); Files.copy(source,migrations.resolve(source.getFileName()));
        var flyway=Flyway.configure().dataSource(url,user,password).locations("filesystem:"+migrations.toAbsolutePath())
                .baselineOnMigrate(true).baselineVersion("12").load();
        assertEquals(1,flyway.migrate().migrationsExecuted); flyway.validate(); assertEquals(0,flyway.migrate().migrationsExecuted);
        try(var c=DriverManager.getConnection(url,user,password)) {
            insert(c,1,"Key"); insert(c,1,"key"); insert(c,2,"Key");
            assertThrows(SQLException.class,()->insert(c,1,"Key"));
            assertThrows(SQLException.class,()->insert(c,99,"foreign"));
            c.setAutoCommit(false); insert(c,1,"rollback"); c.rollback(); c.setAutoCommit(true);
            try(var s=c.createStatement();var r=s.executeQuery("SELECT COUNT(*) FROM api_idempotency")) { assertTrue(r.next()); assertEquals(3,r.getInt(1)); }
            try(var p=c.prepareStatement("UPDATE api_idempotency SET response_status=201,response_body=?,response_location=?,resource_id=7 WHERE tenant_id=1")) {
                p.setString(1,"{\"data\":{\"id\":7}}"); p.setString(2,"/api/v1/invoices/7"); assertEquals(2,p.executeUpdate());
            }
        }
    }
    private void insert(Connection c,long tenant,String key) throws Exception {
        try(var p=c.prepareStatement("INSERT INTO api_idempotency(tenant_id,api_key_id,idempotency_key,request_hash,created_at,expires_at) VALUES(?,1,?,?,CURRENT_TIMESTAMP,?)")) {
            p.setLong(1,tenant);p.setBytes(2,key.getBytes(StandardCharsets.US_ASCII));p.setString(3,"a".repeat(64));
            p.setTimestamp(4,Timestamp.valueOf("2099-01-01 00:00:00"));p.executeUpdate();
        }
    }
}
