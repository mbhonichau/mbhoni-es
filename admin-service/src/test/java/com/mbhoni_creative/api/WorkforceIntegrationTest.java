package com.mbhoni_creative.api;

import com.mbhoni_creative.admincontroller.*;
import com.mbhoni_creative.admincontroller.api.UserProfileApiController;
import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminexception.GlobalExceptionHandler;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.*;
import com.mbhoni_creative.api.config.*;
import com.mbhoni_creative.api.controller.*;
import com.mbhoni_creative.api.error.*;
import com.mbhoni_creative.api.security.*;
import com.mbhoni_creative.api.service.ApiReadService;
import com.mbhoni_creative.billing.*;
import com.mbhoni_creative.config.*;
import com.mbhoni_creative.security.TenantContext;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.security.web.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = WorkforceIntegrationTest.App.class, properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false", "app.stripe.webhook-secret=whsec_test",
        "spring.main.allow-bean-definition-overriding=false", "spring.flyway.enabled=false", "spring.sql.init.mode=never"})
@AutoConfigureMockMvc
class WorkforceIntegrationTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
    @Import({SecurityConfig.class, ApiSecurityConfig.class, ApiOpenApiConfig.class, ApiCredentialService.class,
            ApiKeyAuthenticationFilter.class, TenantContextFilter.class, TenantSecurityService.class, TenantAccessService.class,
            ApiController.class, ApiReadService.class, ApiExceptionHandler.class, ApiDocsController.class, ApiNotFoundController.class,
            EmployeeApiController.class, UserProfileApiController.class, IntegrationApiController.class, GlobalExceptionHandler.class,
            StripeWebhookController.class, StripeWebhookService.class, StripeSignatureVerifier.class, BillingApiErrors.class, com.mbhoni_creative.api.repository.WorkforceRepository.class, com.mbhoni_creative.api.service.WorkforceService.class, WorkforceController.class, WorkforceExceptionHandler.class, TenantQuotaApiController.class})
    static class App {
        @Bean CountingDataSource workforceDataSource() {
            return new CountingDataSource(new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:workforce;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE", "sa", ""));
        }
        @Bean org.springframework.jdbc.core.JdbcTemplate jdbc(CountingDataSource ds) { return new org.springframework.jdbc.core.JdbcTemplate(ds); }
        @Bean org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate named(org.springframework.jdbc.core.JdbcTemplate jdbc) { return new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(jdbc); }
        @Bean org.springframework.transaction.PlatformTransactionManager tx(CountingDataSource ds) { return new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds); }
        @Bean Clock clock() { return Clock.fixed(Instant.parse("2026-11-13T12:00:00Z"), ZoneOffset.UTC); }
    }
    @Autowired MockMvc mvc;
    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("securityFilterChain") SecurityFilterChain legacyChain;
    @Autowired ApiKeyAuthenticationFilter legacyKeyFilter;
    @Autowired TenantContextFilter legacyTenantFilter;
    @MockitoBean ApiKeyService keys;
    @MockitoBean CustomUserDetailsService details;
    @MockitoBean CustomLoginSuccessHandler login;
    @MockitoBean CustomOAuth2UserService oauth;
    @MockitoBean TenantGovernance governance;
    @MockitoBean TenantEntitlementService tenantEntitlementService;
    @MockitoBean EmployeeService employees;
    @MockitoBean UserProfileService profiles;
    @MockitoBean TenantRepository tenants;
    @MockitoBean UserRepository users;
    @MockitoBean TenantSettingsRepository settings;
    @MockitoBean TenantCustomizationRepository branding;
    @MockitoBean TenantQuotaRepository quotas;
    @MockitoBean TenantSubscriptionRepository subscriptions;
    @MockitoBean TenantModuleRepository tenantModules;
    @MockitoBean SubscriptionPlanModuleRepository planModules;
    @MockitoBean ModuleService modules;
    @MockitoBean SubscriptionLifecycle lifecycle;
    @MockitoBean StripeGateway stripe;
    @MockitoBean WebhookReceiptRepository receipts;
    @MockitoBean StripePlanResolver plans;
    @MockitoBean AuditLogService audit;
    @MockitoBean StripeInvoiceService invoices;
    @MockitoBean TenantQuotaService quotaService;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired CountingDataSource counting;
    private Tenant tenant;
    @BeforeEach void setup() {
        tenant = new Tenant(); tenant.setId(1L); tenant.setName("Tenant One"); tenant.setActive(true);
        TenantApiKey key = key("USER_VIEW", "TENANT_VIEW", "CUSTOMIZATION_VIEW", "MODULE_VIEW", "SUBSCRIPTION_VIEW");
        when(keys.authenticate("valid")).thenReturn(key);
        when(keys.authenticate("unknown-permission")).thenReturn(key("USER_DELETE"));
        when(keys.authenticate("tenant-only")).thenReturn(key("TENANT_VIEW"));
        when(tenants.findById(1L)).thenReturn(Optional.of(tenant));
        when(lifecycle.accessState(any())).thenReturn(new SubscriptionLifecycle.AccessState(SubscriptionStatus.ACTIVE, null, null));
        when(tenantEntitlementService.isModuleEnabled(anyString())).thenReturn(true);
        when(keys.authenticate("workforce")).thenReturn(key("API_EMPLOYEE_READ","API_ORG_READ","API_REFERENCE_READ"));
        when(keys.authenticate("mixed")).thenReturn(key("API_EMPLOYEE_READ","USER_VIEW","TENANT_VIEW"));
        Tenant other=new Tenant(); other.setId(2L); other.setName("Other"); other.setActive(true);
        var otherKey=key("API_EMPLOYEE_READ","API_ORG_READ","API_REFERENCE_READ"); otherKey.setTenant(other);
        when(keys.authenticate("other-tenant")).thenReturn(otherKey);
        when(tenants.findById(2L)).thenReturn(Optional.of(other));
        fixtures();
    }
    private TenantApiKey key(String... permissions) {
        var key = new TenantApiKey(); key.setName("Integration"); key.setTenant(tenant);
        for (String name : permissions) { var p = new Permission(); p.setName(name); key.getPermissions().add(p); }
        return key;
    }
    private void stateless(MvcResult result) {
        assertNull(result.getRequest().getSession(false));
        assertNull(result.getResponse().getHeader("Set-Cookie"));
        assertNull(TenantContext.getTenantId());
    }
    static class CountingDataSource extends org.springframework.jdbc.datasource.DelegatingDataSource {
        final java.util.concurrent.atomic.AtomicInteger queries=new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger readOnlyTransactions=new java.util.concurrent.atomic.AtomicInteger();
        CountingDataSource(javax.sql.DataSource target) { super(target); }
        @Override public java.sql.Connection getConnection() throws java.sql.SQLException {
            var connection=super.getConnection();
            return (java.sql.Connection) java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{java.sql.Connection.class},(proxy,method,args)->{
                if(method.getName().equals("prepareStatement")) queries.incrementAndGet();
                if(method.getName().equals("setReadOnly") && Boolean.TRUE.equals(args[0])) readOnlyTransactions.incrementAndGet();
                try { return method.invoke(connection,args); } catch(java.lang.reflect.InvocationTargetException ex) { throw ex.getCause(); }
            });
        }
    }
    private void fixtures() {
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE organization_units(id BIGINT PRIMARY KEY,tenant_id BIGINT,name VARCHAR,unit_code VARCHAR,parent_unit_id BIGINT,unit_type VARCHAR,isolated BOOLEAN,updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE employees(id BIGINT PRIMARY KEY,tenant_id BIGINT,employee_number VARCHAR,first_name VARCHAR,last_name VARCHAR,job_title VARCHAR,status VARCHAR,employment_type VARCHAR,organization_unit_id BIGINT,manager_employee_id BIGINT,hire_date DATE,updated_at TIMESTAMP,national_id VARCHAR,email VARCHAR,residential_address VARCHAR,extended_attributes_json VARCHAR)");
        jdbc.execute("CREATE TABLE field_definitions(id BIGINT PRIMARY KEY,tenant_id BIGINT,field_key VARCHAR,label VARCHAR,section VARCHAR,data_type VARCHAR,is_required BOOLEAN,is_active BOOLEAN,display_order INT,select_options VARCHAR)");
        jdbc.execute("CREATE TABLE employee_field_values(id BIGINT PRIMARY KEY,employee_id BIGINT,field_definition_id BIGINT,value VARCHAR,recorded_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE lookup_categories(id BIGINT PRIMARY KEY,tenant_id BIGINT,code VARCHAR)");
        jdbc.execute("CREATE TABLE lookup_codes(id BIGINT PRIMARY KEY,category_id BIGINT,code VARCHAR,value VARCHAR,locale VARCHAR,active BOOLEAN,metadata VARCHAR)");
        jdbc.update("INSERT INTO organization_units VALUES(1,1,'Visible','V',NULL,'TEAM',false,'2026-01-01'),(2,1,'Hidden','H',NULL,'TEAM',true,'2026-02-01'),(3,1,'Child','C',2,'TEAM',false,'2026-02-01'),(4,2,'Other','O',NULL,'TEAM',false,'2026-02-01'),(5,1,'Cycle','X',6,'TEAM',false,'2026-02-01'),(6,1,'Cycle','Y',5,'TEAM',false,'2026-02-01'),(7,1,'Bad parent','B',4,'TEAM',false,'2026-02-01')");
        for(int i=1;i<=50;i++) jdbc.update("INSERT INTO employees VALUES(?,1,?,'First','Last','Engineer','ACTIVE','FULL_TIME',1,NULL,'2024-01-01','2026-01-01','SECRET_ID','SECRET_EMAIL','SECRET_ADDRESS','SECRET_RAW')",i,"E"+i);
        jdbc.update("INSERT INTO employees VALUES(51,1,'hidden','Hidden','Person',NULL,'ACTIVE','FULL_TIME',2,NULL,NULL,'2026-02-01',NULL,NULL,NULL,NULL),(52,1,'child','Child','Person',NULL,'ACTIVE','FULL_TIME',3,NULL,NULL,'2026-02-01',NULL,NULL,NULL,NULL),(53,2,'foreign','Foreign','Person',NULL,'ACTIVE','FULL_TIME',4,NULL,NULL,'2026-02-01',NULL,NULL,NULL,NULL),(54,1,'unassigned','Unassigned','Person',NULL,'ON_LEAVE','PART_TIME',NULL,51,NULL,'2026-01-01',NULL,NULL,NULL,NULL)");
        jdbc.update("UPDATE employees SET manager_employee_id=53 WHERE id=1");
        jdbc.update("INSERT INTO field_definitions VALUES(1,1,'shirt_size','Shirt size','FIELD_ASSIGNMENT','TEXT',false,true,1,NULL),(2,2,'foreign_field','Foreign','FIELD_ASSIGNMENT','TEXT',false,true,2,NULL),(3,1,'salary','Salary','FIELD_ASSIGNMENT','NUMBER',false,true,3,NULL),(4,NULL,'qualification_type','Qualification / Degree Type','QUALIFICATION','SELECT',false,true,4,'[\"DIPLOMA\"]'),(5,1,'vetting_provider','Vetting','BACKGROUND_CHECK','TEXT',false,true,5,NULL),(6,1,'file','File','FIELD_ASSIGNMENT','FILE',false,true,6,NULL)");
        for(int i=1;i<=50;i++) jdbc.update("INSERT INTO employee_field_values VALUES(?,?,1,'M','2026-01-01')",i,i);
        jdbc.update("INSERT INTO employee_field_values VALUES(51,1,2,'SECRET_FOREIGN','2026-03-01'),(52,1,3,'SECRET_SALARY','2026-01-01'),(53,1,5,'SECRET_VETTING','2026-01-01'),(54,1,6,'SECRET_FILE','2026-01-01'),(55,53,2,'FOREIGN_VALUE','2026-01-01')");
        jdbc.update("INSERT INTO lookup_categories VALUES(1,NULL,'COUNTRY'),(2,1,'COUNTRY'),(3,2,'COUNTRY'),(4,1,'SECRET_CATEGORY')");
        jdbc.update("INSERT INTO lookup_codes VALUES(1,1,'ZA','Global ZA','en',true,'SECRET_META'),(2,2,'ZA','Tenant ZA','en',true,'SECRET_META'),(3,3,'XX','FOREIGN_REFERENCE','en',true,NULL),(4,1,'US','Global US','en',true,NULL),(5,2,'US','Disabled','en',false,NULL)");
    }
    private MvcResult call(String path,String key) throws Exception {
        return mvc.perform(get(path).header("X-API-Key",key)).andReturn();
    }
    private static final List<String> ENDPOINTS=List.of("/api/v1/employees","/api/v1/employees/1","/api/v1/employee-fields","/api/v1/org-units","/api/v1/reference/countries");
    @Test void everyEndpointHasSuccess401403AndStatelessResponses() throws Exception {
        for(String path:ENDPOINTS) {
            stateless(mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(jsonPath("code").value("UNAUTHENTICATED")).andReturn());
            stateless(mvc.perform(get(path).header("X-API-Key","valid")).andExpect(status().isForbidden()).andReturn());
            stateless(mvc.perform(get(path).header("X-API-Key","workforce")).andExpect(status().isOk()).andExpect(jsonPath("data").exists()).andReturn());
        }
    }
    @Test void tenantAndIsolationFiltersNeverLeakRecordsCountsOrManagers() throws Exception {
        mvc.perform(get("/api/v1/employees?size=100").header("X-API-Key","workforce"))
                .andExpect(jsonPath("page.totalElements").value(51)).andExpect(jsonPath("data.length()").value(51))
                .andExpect(jsonPath("data[0].managerId").doesNotExist()).andExpect(jsonPath("data[50].id").value(54))
                .andExpect(jsonPath("data[50].managerId").doesNotExist());
        for(int id:List.of(51,52,53,999)) mvc.perform(get("/api/v1/employees/"+id).header("X-API-Key","workforce"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("code").value("EMPLOYEE_NOT_FOUND"));
        for(int id:List.of(2,3,4,5,6,7,999)) mvc.perform(get("/api/v1/employees?orgUnitId="+id).header("X-API-Key","workforce"))
                .andExpect(status().isOk()).andExpect(jsonPath("page.totalElements").value(0)).andExpect(jsonPath("data").isEmpty());
        mvc.perform(get("/api/v1/org-units").header("X-API-Key","workforce")).andExpect(jsonPath("page.totalElements").value(1)).andExpect(jsonPath("data[0].id").value(1));
        for(String path:ENDPOINTS) {
            String json=call(path,"workforce").getResponse().getContentAsString();
            assertFalse(json.contains("FOREIGN"),path); assertFalse(json.contains("foreign_field"),path);
        }
        mvc.perform(get("/api/v1/employees").header("X-API-Key","other-tenant")).andExpect(jsonPath("page.totalElements").value(1)).andExpect(jsonPath("data[0].id").value(53));
        mvc.perform(get("/api/v1/employees/1").header("X-API-Key","other-tenant")).andExpect(status().isNotFound());
    }
    @Test void sensitiveValuesAndPropertyNamesAreAbsentFromSerializedResponses() throws Exception {
        for(String path:List.of("/api/v1/employees?size=100","/api/v1/employees/1","/api/v1/employee-fields")) {
            String json=call(path,"workforce").getResponse().getContentAsString();
            for(String excluded:List.of("SECRET","nationalId","national_id","taxNumber","email","workPhone","residentialAddress","extendedAttributesJson",
                    "salary","vetting_provider","emergencyContact","backgroundCheckStatus","licenseNumber","serviceProvider","terminationDate","\"file\""))
                assertFalse(json.contains(excluded),path+" contains "+excluded);
        }
        mvc.perform(get("/api/v1/employees/1").header("X-API-Key","workforce")).andExpect(jsonPath("data.customFields.shirt_size").value("M"));
    }
    @Test void paginationCountsAndOrderingAreStableAfterFilters() throws Exception {
        mvc.perform(get("/api/v1/employees?size=25&page=0&orgUnitId=1&status=ACTIVE").header("X-API-Key","workforce"))
                .andExpect(jsonPath("page.totalElements").value(50)).andExpect(jsonPath("data[0].id").value(1)).andExpect(jsonPath("data[24].id").value(25));
        mvc.perform(get("/api/v1/employees?size=25&page=1&orgUnitId=1&status=ACTIVE").header("X-API-Key","workforce"))
                .andExpect(jsonPath("page.totalElements").value(50)).andExpect(jsonPath("data[0].id").value(26));
        mvc.perform(get("/api/v1/employees?status=ON_LEAVE").header("X-API-Key","workforce")).andExpect(jsonPath("page.totalElements").value(1));
        for(String path:List.of("/api/v1/employee-fields","/api/v1/org-units","/api/v1/reference/countries"))
            mvc.perform(get(path+"?size=1&page=999").header("X-API-Key","workforce")).andExpect(jsonPath("data").isEmpty());
    }
    @Test void inclusiveSinceUsesEmployeeOrCustomValueTimestampAndNoForeignValues() throws Exception {
        jdbc.update("UPDATE employees SET updated_at='2026-02-01' WHERE id=1");
        jdbc.update("UPDATE employee_field_values SET recorded_at='2026-02-01' WHERE employee_id=2");
        mvc.perform(get("/api/v1/employees?updatedSince=2026-02-01T00:00:00").header("X-API-Key","workforce"))
                .andExpect(jsonPath("page.totalElements").value(2)).andExpect(jsonPath("data[0].id").value(1)).andExpect(jsonPath("data[1].id").value(2));
        mvc.perform(get("/api/v1/employees?updatedSince=2026-02-01T00:00:01").header("X-API-Key","workforce")).andExpect(jsonPath("page.totalElements").value(0));
        mvc.perform(get("/api/v1/org-units?updatedSince=2026-01-01T00:00:00").header("X-API-Key","workforce")).andExpect(jsonPath("page.totalElements").value(1));
        mvc.perform(get("/api/v1/org-units?updatedSince=2026-01-01T00:00:01").header("X-API-Key","workforce")).andExpect(jsonPath("page.totalElements").value(0));
    }
    @Test void invalidFiltersPagingAndReferenceTypeHaveStableProblemCodes() throws Exception {
        for(String path:List.of("/api/v1/employees?status=UNKNOWN","/api/v1/employees?updatedSince=bad","/api/v1/employees?orgUnitId=no",
                "/api/v1/employees/abc","/api/v1/org-units?updatedSince=bad","/api/v1/org-units?tree=true"))
            mvc.perform(get(path).header("X-API-Key","workforce")).andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("INVALID_FILTER"));
        for(String path:List.of("/api/v1/employees","/api/v1/employee-fields","/api/v1/org-units","/api/v1/reference/countries"))
            for(String query:List.of("?size=101","?size=0","?page=-1","?page=bad"))
                mvc.perform(get(path+query).header("X-API-Key","workforce")).andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("INVALID_PAGE_PARAMS"));
        mvc.perform(get("/api/v1/reference/SECRET_CATEGORY").header("X-API-Key","workforce")).andExpect(status().isNotFound()).andExpect(jsonPath("code").value("REFERENCE_TYPE_NOT_FOUND"));
    }
    @Test void referenceOverridesAndTombstonesAreAppliedWithoutExposingMetadata() throws Exception {
        mvc.perform(get("/api/v1/reference/countries").header("X-API-Key","workforce"))
                .andExpect(jsonPath("page.totalElements").value(1)).andExpect(jsonPath("data[0].value").value("Tenant ZA"))
                .andExpect(jsonPath("data[0].metadata").doesNotExist());
    }
    @Test void fiftyEmployeesHaveTheSameQueryCountAsOneAndGetsDoNotWrite() throws Exception {
        counting.readOnlyTransactions.set(0);
        counting.queries.set(0); assertEquals(200,call("/api/v1/employees?size=1","workforce").getResponse().getStatus()); int one=counting.queries.get();
        counting.queries.set(0); assertEquals(200,call("/api/v1/employees?size=50","workforce").getResponse().getStatus()); int fifty=counting.queries.get();
        assertEquals(5,one); assertEquals(one,fifty);
        System.out.println("WORKFORCE SQL COUNTS: page 1="+one+", page 50="+fifty+" (authentication/tenant repositories mocked)");
        List<String> tables=List.of("employees","organization_units","field_definitions","employee_field_values","lookup_categories","lookup_codes");
        Map<String,List<Map<String,Object>>> before=new LinkedHashMap<>();
        for(String table:tables) before.put(table,jdbc.queryForList("SELECT * FROM "+table+" ORDER BY id"));
        for(String path:ENDPOINTS) assertEquals(200,call(path,"workforce").getResponse().getStatus());
        for(String table:tables) assertEquals(before.get(table),jdbc.queryForList("SELECT * FROM "+table+" ORDER BY id"),table);
        assertTrue(counting.readOnlyTransactions.get()>=ENDPOINTS.size(),"API reads must begin read-only transactions");
    }
    @Test void v1OnlyKeysAre403OnLegacyRoutesAndMixedKeysMatchOriginalLegacyKeys() throws Exception {
        for(String path:List.of("/api/employees?tenantId=1","/api/employees/1","/api/user/profile","/api/user/profile/1",
                "/api/integrations/me","/api/integrations/users","/api/tenants/1/quota"))
            mvc.perform(get(path).header("X-API-Key","workforce")).andExpect(status().isForbidden());
        for(String path:List.of("/api/employees?tenantId=1","/api/user/profile","/api/user/profile/1","/api/integrations/me","/api/tenants/1/quota")) {
            var original=call(path,"valid").getResponse(); var mixed=call(path,"mixed").getResponse();
            assertEquals(original.getStatus(),mixed.getStatus(),path); assertEquals(original.getRedirectedUrl(),mixed.getRedirectedUrl(),path);
            assertEquals(original.getForwardedUrl(),mixed.getForwardedUrl(),path);
        }
        mvc.perform(get("/api/v1/employees").header("X-API-Key","workforce")).andExpect(status().isOk());
    }
    @Test void everyNewEndpointAppearsInTheProtectedOpenApiSpecification() throws Exception {
        mvc.perform(get("/api/openapi.json").header("X-API-Key","workforce"))
                .andExpect(status().isOk()).andExpect(jsonPath("paths['/api/v1/employees']").exists())
                .andExpect(jsonPath("paths['/api/v1/employees/{id}']").exists())
                .andExpect(jsonPath("paths['/api/v1/employee-fields']").exists())
                .andExpect(jsonPath("paths['/api/v1/org-units']").exists())
                .andExpect(jsonPath("paths['/api/v1/reference/{type}']").exists());
    }
    @Test void tenantDefinitionOverridesAreReadOnlyAndCanSuppressGlobalValues() throws Exception {
        jdbc.update("INSERT INTO employee_field_values VALUES(80,1,4,'DIPLOMA','2026-01-01')");
        mvc.perform(get("/api/v1/employees/1").header("X-API-Key","workforce"))
                .andExpect(jsonPath("data.customFields.qualification_type").value("DIPLOMA"));
        jdbc.update("INSERT INTO field_definitions VALUES(20,1,'qualification_type','Qualification','QUALIFICATION','SELECT',false,false,4,'[\"DIPLOMA\"]')");
        mvc.perform(get("/api/v1/employees/1").header("X-API-Key","workforce"))
                .andExpect(jsonPath("data.customFields.qualification_type").doesNotExist());
        mvc.perform(get("/api/v1/employee-fields").header("X-API-Key","workforce"))
                .andExpect(jsonPath("page.totalElements").value(1));
    }
}
