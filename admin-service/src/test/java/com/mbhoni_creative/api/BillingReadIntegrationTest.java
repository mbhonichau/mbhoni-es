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

@SpringBootTest(classes = BillingReadIntegrationTest.App.class, properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false", "app.stripe.webhook-secret=whsec_test",
        "spring.main.allow-bean-definition-overriding=false", "spring.flyway.enabled=false", "spring.sql.init.mode=never"})
@AutoConfigureMockMvc
class BillingReadIntegrationTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
    @Import({SecurityConfig.class, ApiSecurityConfig.class, ApiOpenApiConfig.class, ApiCredentialService.class,
            ApiKeyAuthenticationFilter.class, TenantContextFilter.class, TenantSecurityService.class, TenantAccessService.class,
            ApiController.class, ApiReadService.class, ApiExceptionHandler.class, ApiDocsController.class, ApiNotFoundController.class,
            EmployeeApiController.class, UserProfileApiController.class, IntegrationApiController.class, GlobalExceptionHandler.class,
            StripeWebhookController.class, StripeWebhookService.class, StripeSignatureVerifier.class, BillingApiErrors.class, com.mbhoni_creative.api.repository.WorkforceRepository.class, com.mbhoni_creative.api.service.WorkforceService.class, WorkforceController.class, WorkforceExceptionHandler.class, TenantQuotaApiController.class, BillingController.class, TenantInvoiceViewController.class, BillingReadController.class, BillingReadExceptionHandler.class, com.mbhoni_creative.api.repository.BillingReadRepository.class, com.mbhoni_creative.api.service.ApiBillingReadService.class})
    static class App {
        @Bean CountingDataSource workforceDataSource() {
            return new CountingDataSource(new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:billingreads;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE", "sa", ""));
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
    @MockitoBean BillingService billingService;
    @MockitoBean TenantInvoicingService invoicingService;
    @MockitoBean TenantCustomizationService customization;
    @MockitoBean InvoiceDocumentService documents;
    @MockitoBean TenantService tenantService;
    @MockitoBean BillingAccountRepository accounts;
    @MockitoBean ExpenseRepository expenseRepository;
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
        when(keys.authenticate("billing")).thenReturn(key("API_BILLING_READ"));
        when(keys.authenticate("reserved-write")).thenReturn(key("API_BILLING_WRITE"));
        when(keys.authenticate("billing-old")).thenReturn(key("BILLING_VIEW"));
        when(keys.authenticate("billing-mixed")).thenReturn(key("BILLING_VIEW","API_BILLING_READ"));
        var foreign=key("API_BILLING_READ"); foreign.setTenant(other); when(keys.authenticate("billing-foreign")).thenReturn(foreign);
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
        final java.util.concurrent.atomic.AtomicInteger readOnly=new java.util.concurrent.atomic.AtomicInteger();
        CountingDataSource(javax.sql.DataSource target) { super(target); }
        @Override public java.sql.Connection getConnection() throws java.sql.SQLException {
            var connection=super.getConnection();
            return (java.sql.Connection)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{java.sql.Connection.class},(proxy,method,args)->{
                if(method.getName().equals("prepareStatement")) queries.incrementAndGet();
                if(method.getName().equals("setReadOnly")&&Boolean.TRUE.equals(args[0])) readOnly.incrementAndGet();
                try { return method.invoke(connection,args); } catch(java.lang.reflect.InvocationTargetException ex) { throw ex.getCause(); }
            });
        }
    }
    private void fixtures() {
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE tenants(id BIGINT PRIMARY KEY)"); jdbc.update("INSERT INTO tenants VALUES(1),(2)");
        for(String kind:List.of("invoice","quotation")) {
            String end=kind.equals("invoice")?"due_date":"valid_until_date";
            jdbc.execute("CREATE TABLE tenant_"+kind+"s(id BIGINT PRIMARY KEY,tenant_id BIGINT,"+kind+"_number VARCHAR,customer_name VARCHAR,status VARCHAR,issue_date DATE,"+end+" DATE,subtotal DECIMAL(12,2),tax_amount DECIMAL(12,2),total_amount DECIMAL(12,2),amount_paid DECIMAL(12,2),updated_at TIMESTAMP,notes VARCHAR,customer_tax_number VARCHAR,customer_email VARCHAR,created_by VARCHAR)");
            jdbc.execute("CREATE TABLE tenant_"+kind+"_items(id BIGINT PRIMARY KEY,"+kind+"_id BIGINT,description VARCHAR,quantity DECIMAL(12,2),unit_price DECIMAL(12,2),tax_rate DECIMAL(5,2),total_amount DECIMAL(12,2),updated_at TIMESTAMP)");
            for(int i=1;i<=51;i++) {
                jdbc.update("INSERT INTO tenant_"+kind+"s VALUES(?,?,?,'Customer','DRAFT','2026-01-01','2026-02-01',0.06,0.01,0.07,0.00,'2026-01-01','SECRET_NOTES','SECRET_TAX','SECRET_EMAIL','SECRET_AUDIT')",i,i==51?2:1,kind+"-"+i);
                for(int j=0;j<2;j++) jdbc.update("INSERT INTO tenant_"+kind+"_items VALUES(?,?,'Line',1.00,0.03,15.00,0.03,'2026-01-01')",i*2+j,i);
            }
        }
        jdbc.execute("CREATE TABLE business_expenses(id BIGINT PRIMARY KEY,tenant_id BIGINT,expense_date DATE,vendor_name VARCHAR,expense_category VARCHAR,reference_number VARCHAR,amount DECIMAL(12,2),tax_amount DECIMAL(12,2),updated_at TIMESTAMP,notes VARCHAR,payment_method VARCHAR)");
        jdbc.update("INSERT INTO business_expenses VALUES(1,1,'2026-01-01','Vendor','SUPPLIES','REF1',1250.50,100.00,'2026-01-01','SECRET_NOTES','SECRET_PAYMENT'),(2,2,'2026-01-01','Foreign vendor','SUPPLIES','FOREIGN',25.00,0.00,'2026-01-01',NULL,NULL)");
        try(var c=counting.getConnection()) {
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(c,new org.springframework.core.io.ClassPathResource("db/migration/V13__api_idempotency.sql"));
        } catch(java.sql.SQLException ex) { throw new IllegalStateException(ex); }
    }
    private static final List<String> PATHS=List.of("/api/v1/invoices","/api/v1/invoices/1","/api/v1/quotations","/api/v1/quotations/1","/api/v1/expenses","/api/v1/expenses/1");
    @Test void allReadsRequireBillingScopeAndRemainStateless() throws Exception {
        var multi=new TenantSettings();multi.setMultiCurrencyEnabled(true);
        when(settings.findByTenantId(1L)).thenReturn(Optional.of(multi));
        for(String path:PATHS) {
            stateless(mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(jsonPath("code").value("UNAUTHENTICATED")).andReturn());
            for(String key:List.of("valid","workforce","reserved-write"))
                stateless(mvc.perform(get(path).header("X-API-Key",key)).andExpect(status().isForbidden()).andReturn());
            stateless(mvc.perform(get(path).header("X-API-Key","billing")).andExpect(status().isOk()).andExpect(jsonPath("data").exists()).andReturn());
        }
    }
    @Test void foreignDocumentsAndItemsAreNeverReturned() throws Exception {
        for(String kind:List.of("invoices","quotations")) {
            mvc.perform(get("/api/v1/"+kind+"?size=100").header("X-API-Key","billing")).andExpect(jsonPath("page.totalElements").value(50)).andExpect(jsonPath("data.length()").value(50));
            mvc.perform(get("/api/v1/"+kind+"/51").header("X-API-Key","billing")).andExpect(status().isNotFound());
            mvc.perform(get("/api/v1/"+kind+"/1").header("X-API-Key","billing-foreign")).andExpect(status().isNotFound());
            mvc.perform(get("/api/v1/"+kind).header("X-API-Key","billing-foreign")).andExpect(jsonPath("page.totalElements").value(1)).andExpect(jsonPath("data[0].id").value(51)).andExpect(jsonPath("data[0].items[0].id").value(102));
        }
        mvc.perform(get("/api/v1/expenses/2").header("X-API-Key","billing")).andExpect(status().isNotFound()).andExpect(jsonPath("code").value("EXPENSE_NOT_FOUND"));
        mvc.perform(get("/api/v1/expenses").header("X-API-Key","billing-foreign")).andExpect(jsonPath("data[0].id").value(2));
    }
    @Test void listsHaveStablePagingFiltersAndInclusiveComponentTimestamps() throws Exception {
        for(String kind:List.of("invoice","quotation")) {
            jdbc.update("UPDATE tenant_"+kind+"_items SET updated_at='2026-03-01' WHERE "+kind+"_id=2");
            jdbc.update("UPDATE tenant_"+kind+"s SET updated_at='2026-03-01' WHERE id=3");
            mvc.perform(get("/api/v1/"+kind+"s?size=25&page=1&status=DRAFT&dateFrom=2026-01-01&dateTo=2026-01-01").header("X-API-Key","billing"))
                    .andExpect(jsonPath("page.totalElements").value(50)).andExpect(jsonPath("data[0].id").value(26));
            mvc.perform(get("/api/v1/"+kind+"s?updatedSince=2026-03-01T00:00:00").header("X-API-Key","billing"))
                    .andExpect(jsonPath("page.totalElements").value(2)).andExpect(jsonPath("data[0].id").value(2)).andExpect(jsonPath("data[0].updatedAt").value("2026-03-01T00:00:00"));
            mvc.perform(get("/api/v1/"+kind+"s?updatedSince=2026-03-01T00:00:01").header("X-API-Key","billing")).andExpect(jsonPath("data").isEmpty());
        }
        mvc.perform(get("/api/v1/expenses?dateFrom=2026-01-02").header("X-API-Key","billing")).andExpect(jsonPath("page.totalElements").value(0));
        mvc.perform(get("/api/v1/expenses?updatedSince=2026-01-01T00:00:00").header("X-API-Key","billing")).andExpect(jsonPath("page.totalElements").value(1));
    }
    @Test void invalidFiltersAndMissingIdsUseStableProblemCodes() throws Exception {
        for(String kind:List.of("invoices","quotations","expenses")) {
            for(String query:List.of("?size=101","?size=0","?page=-1","?page=abc"))
                mvc.perform(get("/api/v1/"+kind+query).header("X-API-Key","billing")).andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("INVALID_PAGE_PARAMS"));
            for(String query:List.of("?status=NOPE","?updatedSince=bad","?dateFrom=bad","?dateFrom=2026-02-01&dateTo=2026-01-01","?customerId=2","?tenantId=2","?status=DRAFT&status=PAID"))
                mvc.perform(get("/api/v1/"+kind+query).header("X-API-Key","billing")).andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("INVALID_FILTER"));
            mvc.perform(get("/api/v1/"+kind+"/999").header("X-API-Key","billing")).andExpect(status().isNotFound());
            mvc.perform(get("/api/v1/"+kind+"/bad").header("X-API-Key","billing")).andExpect(status().isBadRequest());
            mvc.perform(get("/api/v1/"+kind+"?page=999").header("X-API-Key","billing")).andExpect(jsonPath("data").isEmpty());
        }
    }
    @Test void storedMoneyAndRoundingRoundTripExactlyWithoutSensitiveFieldsOrCurrencyInference() throws Exception {
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        for(String path:PATHS) {
            String json=mvc.perform(get(path).header("X-API-Key","billing")).andReturn().getResponse().getContentAsString();
            for(String forbidden:List.of("SECRET","notes","customerEmail","customerTaxNumber","createdBy","updatedBy","paymentMethod","stripe","currency","bankAccount")) assertFalse(json.contains(forbidden),path+" "+forbidden);
            var data=mapper.readTree(json).get("data"); var record=data.isArray()?data.get(0):data;
            if(path.contains("expenses")) { assertTrue(record.get("amount").isTextual()); assertEquals("1250.50",record.get("amount").asText()); }
            else {
                assertTrue(record.get("totalAmount").isTextual()); assertEquals("0.07",record.get("totalAmount").asText());
                assertEquals("0.03",record.get("items").get(0).get("totalAmount").asText());
                assertEquals("15.00",record.get("items").get(0).get("taxRate").asText());
            }
        }
    }
    @Test void listsWithLinesUseThreeQueriesForBothOneAndFiftyAndReadsNeverWrite() throws Exception {
        for(String kind:List.of("invoices","quotations")) {
            counting.queries.set(0);mvc.perform(get("/api/v1/"+kind+"?size=1").header("X-API-Key","billing")).andExpect(status().isOk());int one=counting.queries.get();
            counting.queries.set(0);mvc.perform(get("/api/v1/"+kind+"?size=50").header("X-API-Key","billing")).andExpect(status().isOk());int fifty=counting.queries.get();
            assertEquals(3,one);assertEquals(one,fifty);System.out.println("BILLING QUERY COUNTS "+kind+": 1="+one+", 50="+fifty+"; auth/tenant repositories mocked");
        }
        List<String> tables=List.of("tenant_invoices","tenant_invoice_items","tenant_quotations","tenant_quotation_items","business_expenses","api_idempotency");
        Map<String,List<Map<String,Object>>> before=new HashMap<>(); for(String table:tables) before.put(table,jdbc.queryForList("SELECT * FROM "+table+" ORDER BY id"));
        counting.readOnly.set(0);for(String path:PATHS) mvc.perform(get(path).header("X-API-Key","billing")).andExpect(status().isOk());
        assertTrue(counting.readOnly.get()>=6);
        for(String table:tables) assertEquals(before.get(table),jdbc.queryForList("SELECT * FROM "+table+" ORDER BY id"));
    }
    @Test void everyLegacyBillingRouteRejectsBillingOnlyKeysEvenWhenModulesAreEnabled() throws Exception {
        for(Class<?> controller:List.of(BillingController.class,TenantInvoiceViewController.class)) {
            String prefix=controller.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class).value()[0];
            for(var method:controller.getDeclaredMethods()) {
                var get=method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class);
                var post=method.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class);
                if(get==null&&post==null) continue;
                String[] paths=get!=null?get.value():post.value();
                if(paths.length==0) paths=new String[]{""};
                for(String path:paths) for(String key:List.of("billing","reserved-write")) {
                    String url=prefix+path.replaceAll("\\{[^}]+}","1");
                    var request=get!=null?get(url):post(url).with(csrf());
                    mvc.perform(request.header("X-API-Key",key)).andExpect(status().isForbidden());
                }
            }
        }
        verifyNoInteractions(billingService,invoicingService,documents);
    }
    @Test void oldAndMixedKeysKeepLegacyBehaviorAndBillingWritesRemainUnavailable() throws Exception {
        for(String path:List.of("/billing/accounts","/billing/invoices","/billing/expenses","/tenant/invoices","/tenant/quotations","/tenant/invoices/builder")) {
            var old=mvc.perform(get(path).header("X-API-Key","billing-old")).andReturn().getResponse();
            var mixed=mvc.perform(get(path).header("X-API-Key","billing-mixed")).andReturn().getResponse();
            assertEquals(old.getStatus(),mixed.getStatus(),path);assertEquals(old.getForwardedUrl(),mixed.getForwardedUrl(),path);assertEquals(old.getRedirectedUrl(),mixed.getRedirectedUrl(),path);
        }
        for(String kind:List.of("invoices","quotations","expenses")) {
            int status=mvc.perform(post("/api/v1/"+kind).header("X-API-Key","billing").contentType("application/json").content("{}")).andReturn().getResponse().getStatus();
            assertTrue(status>=400,"Phase 3a must not expose writes");
        }
    }
    @Test void openApiExposesAllReadsAndNoBillingPosts() throws Exception {
        var json=mvc.perform(get("/api/openapi.json").header("X-API-Key","billing")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var paths=new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("paths");
        for(String path:List.of("/api/v1/invoices","/api/v1/invoices/{id}","/api/v1/quotations","/api/v1/quotations/{id}","/api/v1/expenses","/api/v1/expenses/{id}")) {
            assertTrue(paths.get(path).has("get"));assertFalse(paths.get(path).has("post"));
        }
    }
}
