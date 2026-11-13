package com.mbhoni_creative.billing;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.aop.framework.ProxyFactory;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantPurgeServiceTest {
    final Instant now = Instant.parse("2026-09-26T00:00:00Z");
    final TenantSubscriptionRepository subscriptions = mock(TenantSubscriptionRepository.class);
    JdbcTemplate jdbc; TenantPurgeService service; TenantSubscription subscription;
    @BeforeEach void setup() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:purge" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE tenants(id BIGINT PRIMARY KEY, owner_user_id BIGINT)");
        jdbc.execute("CREATE TABLE tenant_subscriptions(id BIGINT PRIMARY KEY, tenant_id BIGINT REFERENCES tenants(id))");
        jdbc.execute("CREATE TABLE users(id BIGINT PRIMARY KEY, tenant_id BIGINT REFERENCES tenants(id))");
        jdbc.execute("ALTER TABLE tenants ADD FOREIGN KEY(owner_user_id) REFERENCES users(id)");
        jdbc.execute("CREATE TABLE roles(id BIGINT PRIMARY KEY, tenant_id BIGINT REFERENCES tenants(id))");
        jdbc.execute("CREATE TABLE user_roles(user_id BIGINT REFERENCES users(id) ON DELETE CASCADE, role_id BIGINT REFERENCES roles(id) ON DELETE CASCADE)");
        jdbc.execute("CREATE TABLE organization_units(id BIGINT PRIMARY KEY, tenant_id BIGINT REFERENCES tenants(id), parent_id BIGINT REFERENCES organization_units(id))");
        jdbc.execute("CREATE TABLE invoices(id BIGINT PRIMARY KEY, tenant_id BIGINT REFERENCES tenants(id))");
        jdbc.execute("CREATE TABLE payments(id BIGINT PRIMARY KEY, invoice_id BIGINT REFERENCES invoices(id))");
        jdbc.execute("CREATE TABLE audit_logs(id BIGINT PRIMARY KEY, tenant_id BIGINT)");
        // Both legacy and current service names must be covered through actual FK ownership.
        jdbc.execute("CREATE TABLE service_catalog(id BIGINT PRIMARY KEY, tenant_id BIGINT REFERENCES tenants(id))");
        jdbc.execute("CREATE TABLE service_catalogs(id BIGINT PRIMARY KEY, tenant_id BIGINT REFERENCES tenants(id))");
        jdbc.execute("CREATE TABLE service_slas(id BIGINT PRIMARY KEY, service_id BIGINT REFERENCES service_catalog(id))");
        jdbc.update("INSERT INTO tenants VALUES(1,NULL),(2,NULL)");
        jdbc.update("INSERT INTO tenant_subscriptions VALUES(1,1),(2,2)");
        jdbc.update("INSERT INTO users VALUES(10,1),(20,2)");
        jdbc.update("UPDATE tenants SET owner_user_id = id * 10");
        jdbc.update("INSERT INTO roles VALUES(1,NULL),(10,1),(20,2)");
        jdbc.update("INSERT INTO user_roles VALUES(10,1),(10,10),(20,1),(20,20)");
        jdbc.update("INSERT INTO organization_units VALUES(10,1,NULL),(11,1,10),(20,2,NULL)");
        for (String table : List.of("invoices", "audit_logs", "service_catalog", "service_catalogs"))
            jdbc.update("INSERT INTO " + table + " VALUES(10,1),(20,2)");
        jdbc.update("INSERT INTO payments VALUES(10,10),(20,20)"); jdbc.update("INSERT INTO service_slas VALUES(10,10),(20,20)");
        subscription = new TenantSubscription(); subscription.setStatus(SubscriptionStatus.CANCELLED); subscription.setRetentionEndsAt(now);
        when(subscriptions.lockByTenantId(1L)).thenReturn(Optional.of(subscription));
        var target = new TenantPurgeService(subscriptions, new TenantDataScope(jdbc), jdbc, Clock.fixed(now, ZoneOffset.UTC), mock(EntityManager.class));
        var interceptor = new TransactionInterceptor(); interceptor.setTransactionManager(new DataSourceTransactionManager(source));
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        var factory = new ProxyFactory(target); factory.setProxyTargetClass(true); factory.addAdvice(interceptor);
        service = (TenantPurgeService) factory.getProxy();
    }
    int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    @Test void dayThirtyDeletesOwnedDataIncludingBillingAuditAndLegacyChildrenOnly() {
        assertTrue(service.purgeExpired(1L));
        for (String table : List.of("tenants", "tenant_subscriptions", "users", "organization_units", "invoices", "payments", "audit_logs", "service_catalog", "service_catalogs", "service_slas"))
            assertEquals(1, count(table), table);
        assertEquals(2, count("roles")); assertEquals(2, count("user_roles"));
        assertEquals(20L, jdbc.queryForObject("SELECT owner_user_id FROM tenants WHERE id=2", Long.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs WHERE tenant_id=1", Integer.class));
    }
    @Test void missingDeadlineActiveTenantAndUnexpiredRetentionCannotBePurged() {
        subscription.setRetentionEndsAt(now.plusSeconds(1)); assertFalse(service.purgeExpired(1L));
        subscription.setRetentionEndsAt(null); assertFalse(service.purgeExpired(1L));
        subscription.setRetentionEndsAt(now.minusSeconds(1)); subscription.setStatus(SubscriptionStatus.ACTIVE); assertFalse(service.purgeExpired(1L));
        assertEquals(2, count("tenants")); assertEquals(2, count("audit_logs"));
    }
    @Test void inconsistentCrossTenantJoinAbortsAndRollsBackEveryDeletion() {
        jdbc.update("INSERT INTO user_roles VALUES(20,10)");
        assertThrows(RuntimeException.class, () -> service.purgeExpired(1L));
        assertEquals(2, count("tenants")); assertEquals(2, count("audit_logs")); assertEquals(5, count("user_roles"));
        assertEquals(10L, jdbc.queryForObject("SELECT owner_user_id FROM tenants WHERE id=1", Long.class));
    }
    @Test void directlyScopedForeignChildAlsoBlocksCascadingDeletion() {
        jdbc.execute("CREATE TABLE dependent_notes(id BIGINT PRIMARY KEY, tenant_id BIGINT, invoice_id BIGINT REFERENCES invoices(id) ON DELETE CASCADE)");
        jdbc.update("INSERT INTO dependent_notes VALUES(1,2,10)");
        assertThrows(IllegalStateException.class, () -> service.purgeExpired(1L));
        assertEquals(2, count("tenants")); assertEquals(1, count("dependent_notes")); assertEquals(2, count("audit_logs"));
    }
    @Test void lateDatabaseFailureRollsBackPriorDeletesAndOwnerUnlink() {
        jdbc.execute("CREATE TRIGGER fail_purge BEFORE DELETE ON tenants FOR EACH ROW CALL 'com.mbhoni_creative.billing.TenantPurgeServiceTest$RejectTenantDelete'");
        assertThrows(RuntimeException.class, () -> service.purgeExpired(1L));
        assertEquals(2, count("users")); assertEquals(2, count("audit_logs"));
        assertEquals(10L, jdbc.queryForObject("SELECT owner_user_id FROM tenants WHERE id=1", Long.class));
    }
    public static class RejectTenantDelete implements org.h2.api.Trigger {
        public void fire(java.sql.Connection connection, Object[] oldRow, Object[] newRow) throws java.sql.SQLException {
            throw new java.sql.SQLException("Injected final deletion failure");
        }
    }
}
