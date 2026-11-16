package com.mbhoni_creative.billing;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class TenantExportScopeTest {
    @Test void actualSchemaScopesChildrenAndNeverExportsSecretsOrSharedCatalogAsTenantRows() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:export" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE tenants(id BIGINT PRIMARY KEY)");
        jdbc.execute("CREATE TABLE tenant_subscriptions(id BIGINT PRIMARY KEY, tenant_id BIGINT)");
        jdbc.execute("CREATE TABLE users(id BIGINT PRIMARY KEY, username VARCHAR, email VARCHAR, first_name VARCHAR, last_name VARCHAR, active BOOLEAN, tenant_id BIGINT, password VARCHAR, password_reset_token VARCHAR)");
        jdbc.execute("CREATE TABLE contracts(id BIGINT PRIMARY KEY, tenant_id BIGINT)");
        jdbc.execute("CREATE TABLE contract_milestones(id BIGINT PRIMARY KEY, contract_id BIGINT REFERENCES contracts(id))");
        jdbc.execute("CREATE TABLE service_catalogs(id BIGINT PRIMARY KEY, service_code VARCHAR)");
        jdbc.execute("CREATE TABLE tenant_api_keys(id BIGINT PRIMARY KEY, tenant_id BIGINT, secret VARCHAR)");
        jdbc.update("INSERT INTO users VALUES(10,'a','a@example.com','A','User',true,1,'secret','reset'),(20,'b','b@example.com','B','User',true,2,'secret','reset')");
        jdbc.update("INSERT INTO contracts VALUES(10,1),(20,2)");
        jdbc.update("INSERT INTO contract_milestones VALUES(10,10),(20,20)");
        var queries = TenantExportController.exportQueries(new TenantDataScope(jdbc).inspect());
        assertFalse(queries.containsKey("tenant_api_keys")); assertFalse(queries.containsKey("service_catalogs"));
        var users = queries.get("users"); var result = jdbc.queryForList(users.sql(), users.scope().arguments(1L));
        assertEquals(1, result.size()); assertFalse(result.get(0).keySet().stream().anyMatch(key -> key.toLowerCase().contains("password")));
        var milestones = queries.get("contract_milestones");
        var children = jdbc.queryForList(milestones.sql(), milestones.scope().arguments(1L));
        assertEquals(1, children.size()); assertEquals(10L, children.get(0).get("ID"));
    }
}
