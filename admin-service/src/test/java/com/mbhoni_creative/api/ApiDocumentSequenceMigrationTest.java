package com.mbhoni_creative.api;

import java.nio.file.*;
import java.sql.*;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ApiDocumentSequenceMigrationTest {
    @TempDir Path migrations;
    @Test void v14IsAdditivePortableAndLocksAllocationAtomically() throws Exception {
        for(String name:new String[]{"V13__api_idempotency.sql","V14__api_document_sequence.sql"})
            Files.copy(Path.of("src/main/resources/db/migration",name),migrations.resolve(name));
        verify("jdbc:h2:mem:sequence_"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa");
        // This test always runs on H2. The existing opt-in enables its additional MySQL execution without adding another skipped test.
        if("true".equals(System.getenv("V8_MYSQL_INTEGRATION"))) {
            String server="jdbc:mysql://127.0.0.1:13368/",schema="v14_test_"+UUID.randomUUID().toString().replace("-","");
            try(var c=DriverManager.getConnection(server,"root","");var s=c.createStatement()) {
                try(var r=s.executeQuery("SELECT @@datadir")) { assertTrue(r.next());assertEquals(Path.of("target/v8-mysql-regression").toAbsolutePath().normalize(),Path.of(r.getString(1)).toAbsolutePath().normalize()); }
                s.execute("CREATE DATABASE "+schema);
                try { verify(server+schema,"root"); } finally { s.execute("DROP DATABASE "+schema); }
            }
        }
    }
    private void verify(String url,String user) throws Exception {
        try(var c=DriverManager.getConnection(url,user,"");var s=c.createStatement()) {
            s.execute("CREATE TABLE tenants(id BIGINT PRIMARY KEY)");s.execute("INSERT INTO tenants VALUES(1),(2)");
        }
        var flyway=Flyway.configure().dataSource(url,user,"").locations("filesystem:"+migrations.toAbsolutePath()).baselineOnMigrate(true).baselineVersion("12").load();
        assertEquals(2,flyway.migrate().migrationsExecuted);flyway.validate();assertEquals(0,flyway.migrate().migrationsExecuted);
        try(var a=DriverManager.getConnection(url,user,"");var b=DriverManager.getConnection(url,user,"");var s=a.createStatement();var t=b.createStatement()) {
            s.execute("INSERT INTO api_document_sequence VALUES(1,'INVOICE',1),(1,'QUOTATION',1),(2,'INVOICE',1)");
            assertThrows(SQLException.class,()->s.execute("INSERT INTO api_document_sequence VALUES(1,'INVOICE',2)"));
            assertThrows(SQLException.class,()->s.execute("INSERT INTO api_document_sequence VALUES(1,'EXPENSE',1)"));
            a.setAutoCommit(false);b.setAutoCommit(false);
            try(var r=s.executeQuery("SELECT id FROM tenants WHERE id=1 FOR UPDATE NOWAIT")) { assertTrue(r.next()); }
            assertThrows(SQLException.class,()->t.executeQuery("SELECT id FROM tenants WHERE id=1 FOR UPDATE NOWAIT"));b.rollback();
            try(var r=s.executeQuery("SELECT next_value FROM api_document_sequence WHERE tenant_id=1 AND document_type='INVOICE' FOR UPDATE")) { assertTrue(r.next());assertEquals(1,r.getLong(1)); }
            s.execute("UPDATE api_document_sequence SET next_value=2 WHERE tenant_id=1 AND document_type='INVOICE'");
            s.execute("INSERT INTO api_idempotency(tenant_id,api_key_id,idempotency_key,request_hash,created_at,expires_at) VALUES(1,1,X'41','"+"a".repeat(64)+"',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)");
            a.rollback();a.setAutoCommit(true);
            try(var r=s.executeQuery("SELECT next_value FROM api_document_sequence WHERE tenant_id=1 AND document_type='INVOICE'")) { assertTrue(r.next());assertEquals(1,r.getLong(1)); }
            try(var r=s.executeQuery("SELECT COUNT(*) FROM api_idempotency")) { assertTrue(r.next());assertEquals(0,r.getInt(1)); }
        }
    }
}
