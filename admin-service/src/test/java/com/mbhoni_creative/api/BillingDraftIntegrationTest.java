package com.mbhoni_creative.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.*;
import com.mbhoni_creative.adminservice.impl.TenantInvoicingServiceImpl;
import com.mbhoni_creative.api.config.*;
import com.mbhoni_creative.api.controller.*;
import com.mbhoni_creative.api.error.*;
import com.mbhoni_creative.api.security.*;
import com.mbhoni_creative.api.service.*;
import com.mbhoni_creative.billing.SubscriptionLifecycle;
import com.mbhoni_creative.config.*;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes=BillingDraftIntegrationTest.App.class,properties={
    "spring.cloud.config.enabled=false","eureka.client.enabled=false","spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop","spring.datasource.url=jdbc:h2:mem:drafts;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
    "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
    "spring.jpa.open-in-view=false"})
@AutoConfigureMockMvc
class BillingDraftIntegrationTest {
    @SpringBootConfiguration @EnableAutoConfiguration @EnableMethodSecurity
    @EntityScan("com.mbhoni_creative.adminentity") @EnableJpaRepositories("com.mbhoni_creative.adminrepository")
    @Import({ApiSecurityConfig.class,JpaAuditConfig.class,ApiReadService.class,ApiBillingWriteService.class,BillingDraftParser.class,
        BillingWriteController.class,BillingWriteExceptionHandler.class,ApiExceptionHandler.class,ApiNotFoundController.class,
        ApiController.class,BillingReadController.class,BillingReadExceptionHandler.class,ApiBillingReadService.class,
        com.mbhoni_creative.api.repository.BillingReadRepository.class,ApiIdempotencyCleanup.class})
    static class App {
        @Bean Clock clock() { return Clock.fixed(Instant.parse("2026-11-15T12:00:00Z"),ZoneOffset.UTC); }
        @Bean BillingReadIntegrationTest.CountingDataSource dataSource() {
            return new BillingReadIntegrationTest.CountingDataSource(new org.springframework.jdbc.datasource.DriverManagerDataSource(
                    "jdbc:h2:mem:drafts;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE","sa",""));
        }
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired TenantRepository tenants;
    @Autowired TenantSettingsRepository settings;
    @Autowired TenantInvoiceRepository invoices;
    @Autowired TenantQuotationRepository quotations;
    @Autowired ObjectMapper mapper;
    @Autowired ApiIdempotencyCleanup cleanup;
    @Autowired BillingReadIntegrationTest.CountingDataSource counting;
    @MockitoBean ApiCredentialService credentials;
    @MockitoBean BillingWriteSettings config;
    @MockitoBean SubscriptionLifecycle lifecycle;
    @MockitoBean ModuleService modules;
    @MockitoBean AuditLogService audit;
    Tenant tenant,other;
    @BeforeEach void setup() {
        jdbc.execute("DROP TABLE IF EXISTS api_idempotency");jdbc.execute("DROP TABLE IF EXISTS api_document_sequence");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V13__api_idempotency.sql"),new ClassPathResource("db/migration/V14__api_document_sequence.sql")).execute(jdbc.getDataSource());
        jdbc.update("DELETE FROM tenant_invoice_items");jdbc.update("DELETE FROM tenant_quotation_items");
        jdbc.update("DELETE FROM tenant_invoices");jdbc.update("DELETE FROM tenant_quotations");
        tenant=new Tenant();tenant.setName("Draft tenant");tenant=tenants.saveAndFlush(tenant);
        other=new Tenant();other.setName("Other tenant");other=tenants.saveAndFlush(other);
        when(config.enabled()).thenReturn(true);
        when(lifecycle.accessState(any())).thenReturn(new SubscriptionLifecycle.AccessState(SubscriptionStatus.ACTIVE,null,null));
        when(credentials.authenticate("write")).thenReturn(principal(tenant,"API_BILLING_WRITE"));
        when(credentials.authenticate("read")).thenReturn(principal(tenant,"API_BILLING_READ"));
        when(credentials.authenticate("both")).thenReturn(principal(tenant,"API_BILLING_WRITE","API_BILLING_READ"));
        when(credentials.authenticate("other")).thenReturn(principal(other,"API_BILLING_WRITE"));
    }
    private ApiKeyPrincipal principal(Tenant t,String... names) {
        var k=new TenantApiKey();ReflectionTestUtils.setField(k,"id",42L);k.setTenant(t);k.setName("Integration");
        for(String name:names) { var p=new Permission();p.setName(name);k.getPermissions().add(p); }return new ApiKeyPrincipal(k);
    }
    String body(boolean invoice) { return "{\"customerName\":\"Customer\",\"issueDate\":\"2026-11-15\",\""+(invoice?"dueDate":"validUntilDate")+"\":\"2026-12-15\",\"items\":[{\"description\":\"Line\",\"quantity\":\"1.00\",\"unitPrice\":\"0.03\"}]}"; }
    String path(boolean invoice) { return invoice?"/api/v1/invoices":"/api/v1/quotations"; }
    ResultActions postDraft(boolean invoice,String credential,String key,String body) throws Exception {
        var req=post(path(invoice)).contentType("application/json").content(body);
        if(credential!=null) req.header("X-API-Key",credential);if(key!=null) req.header("Idempotency-Key",key);
        return mvc.perform(req);
    }
    @Test void bothPostsValidateAuthorizeAttributeAndReplayWithoutSessions() throws Exception {
        for(boolean invoice:List.of(true,false)) {
            String b=body(invoice),key="create-"+invoice;
            postDraft(invoice,null,key,b).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
            postDraft(invoice,"read",key,b).andExpect(status().isForbidden());
            when(config.enabled()).thenReturn(false);
            postDraft(invoice,"write",key,b).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("API_WRITES_DISABLED"));
            when(config.enabled()).thenReturn(true);
            postDraft(invoice,"write",null,b).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REQUIRED"));
            var first=postDraft(invoice,"write",key,b).andExpect(status().isCreated()).andExpect(jsonPath("$.data.status").value("DRAFT"))
                    .andExpect(jsonPath("$.data.totalAmount").value("0.03")).andExpect(header().doesNotExist("Set-Cookie")).andReturn();
            assertNull(first.getRequest().getSession(false));
            var replay=postDraft(invoice,"write",key,b).andExpect(status().isCreated()).andReturn();
            assertEquals(first.getResponse().getContentAsString(),replay.getResponse().getContentAsString());
            assertEquals(first.getResponse().getHeader("Location"),replay.getResponse().getHeader("Location"));
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM tenant_"+(invoice?"invoices":"quotations")+" WHERE tenant_id=?",Integer.class,tenant.getId()));
            assertEquals("API_KEY:42",jdbc.queryForObject("SELECT created_by FROM tenant_"+(invoice?"invoices":"quotations")+" WHERE tenant_id=?",String.class,tenant.getId()));
            postDraft(invoice,"write",key,b.replace("Customer","Changed")).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
        }
        verify(audit,times(2)).logForTenant(eq(tenant.getId()),eq("API_DRAFT_CREATED"),anyString(),anyString(),contains("apiKeyId=42"));
    }
    @Test void malformedUnknownControlledFieldsMoneyAndMulticurrencyFailWithoutWrites() throws Exception {
        for(boolean invoice:List.of(true,false)) {
            postDraft(invoice,"write","bad","{").andExpect(status().isBadRequest());
            for(String field:List.of("id","tenant","number","status","totals","taxAmount","createdBy","updatedAt","currency","taxRate","unexpected"))
                postDraft(invoice,"write","bad",body(invoice).replaceFirst("\\{","{\""+field+"\":\"x\",")).andExpect(status().isBadRequest());
            for(String value:List.of("-1","0.001","10000000000","1e2"))
                postDraft(invoice,"write","bad",body(invoice).replace("0.03",value)).andExpect(status().isUnprocessableEntity());
            postDraft(invoice,"write","bad",body(invoice).replace("\"0.03\"","0.03")).andExpect(status().isBadRequest());
            postDraft(invoice,"write","bad",body(invoice).replace("1.00","9999999999.99").replace("0.03","9999999999.99")).andExpect(status().isUnprocessableEntity());
        }
        var s=new TenantSettings();s.setTenant(tenant);s.setMultiCurrencyEnabled(true);settings.saveAndFlush(s);
        for(boolean invoice:List.of(true,false)) postDraft(invoice,"write","currency",body(invoice)).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("MULTI_CURRENCY_TENANT_UNSUPPORTED"));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM api_idempotency",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM api_document_sequence",Integer.class));
    }
    @Test void writeQueryCountsAreBoundedAndReplayDoesNotInsert() throws Exception {
        for(boolean invoice:List.of(true,false)) for(int size:List.of(1,50,200)) {
            var tree=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(body(invoice));var lines=mapper.createArrayNode();
            for(int i=0;i<size;i++) lines.add(mapper.readTree("{\"description\":\"x\",\"quantity\":\"1\",\"unitPrice\":\"0.03\"}"));
            tree.set("items",lines);String json=mapper.writeValueAsString(tree),key="queries-"+invoice+size;
            counting.queries.set(0);postDraft(invoice,"write",key,json).andExpect(status().isCreated());int creates=counting.queries.get();
            assertTrue(creates<=size+20,"Unexpected per-line reads: "+creates);
            counting.queries.set(0);postDraft(invoice,"write",key,json).andExpect(status().isCreated());int replays=counting.queries.get();
            assertTrue(replays<=10,"Replay query count: "+replays);
            System.out.println("DRAFT QUERY COUNTS invoice="+invoice+" lines="+size+" create="+creates+" replay="+replays+"; credential and audit service mocked");
        }
    }
    @Test void decimalBoundariesLineLimitsCanonicalReplayAndLazyExpiry() throws Exception {
        for(boolean invoice:List.of(true,false)) {
            String key="limits-"+invoice;
            String zero=body(invoice).replace("1.00","0.00").replace("0.03","9999999999.99");
            var first=postDraft(invoice,"write",key,zero).andExpect(status().isCreated()).andExpect(jsonPath("$.data.totalAmount").value("0.00")).andReturn();
            postDraft(invoice,"write",key,mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(zero)))
                    .andExpect(status().isCreated()).andExpect(content().string(first.getResponse().getContentAsString()));
            postDraft(invoice,"write","negative",body(invoice).replace("1.00","-0.01")).andExpect(status().isUnprocessableEntity());
            var tree=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(body(invoice));var lines=mapper.createArrayNode();
            for(int i=0;i<201;i++) lines.add(mapper.readTree("{\"description\":\"x\",\"quantity\":\"1\",\"unitPrice\":\"0\"}"));
            tree.set("items",lines);postDraft(invoice,"write","too-many",mapper.writeValueAsString(tree)).andExpect(status().isUnprocessableEntity());
            jdbc.update("UPDATE api_idempotency SET expires_at='2026-11-15 12:00:00' WHERE tenant_id=?",tenant.getId());
            var second=postDraft(invoice,"write",key,zero).andExpect(status().isCreated()).andReturn();
            assertNotEquals(first.getResponse().getHeader("Location"),second.getResponse().getHeader("Location"));
        }
        postDraft(true,"write","cross-endpoint",body(true)).andExpect(status().isCreated());
        postDraft(false,"write","cross-endpoint",body(false)).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }
    @Test void writeOnlyScopeCannotReadOrCallAnyOtherV1Route() throws Exception {
        for(String route:List.of("/users/me","/users/1","/tenant","/tenant/branding","/tenant/quotas","/tenant/modules","/tenant/entitlements/test",
                "/employees","/employees/1","/employee-fields","/org-units","/reference","/reference/employment-types","/invoices","/invoices/1","/quotations","/quotations/1","/expenses","/expenses/1","/unknown"))
            mvc.perform(get("/api/v1"+route).header("X-API-Key","write")).andExpect(status().isForbidden());
        for(String route:List.of("/expenses","/access/check","/invoices/1","/quotations/1","/unknown"))
            mvc.perform(post("/api/v1"+route).header("X-API-Key","write").contentType("application/json").content("{}")).andExpect(status().isForbidden());
        for(String method:List.of("PUT","PATCH","DELETE"))
            mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method),"/api/v1/invoices").header("X-API-Key","write")).andExpect(status().isForbidden());
        postDraft(true,"both","both",body(true)).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/invoices").header("X-API-Key","both")).andExpect(status().isOk());
    }
    @Test void tenantIsolationExpiryAndAtomicRollback() throws Exception {
        var a=postDraft(true,"write","shared",body(true)).andExpect(status().isCreated()).andReturn();
        var b=postDraft(true,"other","shared",body(true)).andExpect(status().isCreated()).andReturn();
        assertNotEquals(a.getResponse().getHeader("Location"),b.getResponse().getHeader("Location"));
        assertEquals(2,jdbc.queryForObject("SELECT COUNT(*) FROM api_idempotency",Integer.class));
        jdbc.update("UPDATE api_idempotency SET expires_at='2026-11-14 12:00:00'");cleanup.expire();
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM api_idempotency",Integer.class));
        postDraft(true,"write","shared",body(true)).andExpect(status().isCreated()).andExpect(jsonPath("$.data.number").value("API-INV-000002"));
        doThrow(new IllegalStateException("audit unavailable")).when(audit).logForTenant(anyLong(),anyString(),anyString(),anyString(),anyString());
        postDraft(true,"write","rollback",body(true)).andExpect(status().isInternalServerError());
        assertEquals(2,invoices.countByTenantId(tenant.getId()));
        assertEquals(3L,jdbc.queryForObject("SELECT next_value FROM api_document_sequence WHERE tenant_id=? AND document_type='INVOICE'",Long.class,tenant.getId()));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM api_idempotency WHERE idempotency_key=?",Integer.class,"rollback".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));
    }
    @Test void inFlightDuplicateConflictsAndConcurrentIdenticalRequestsCreateExactlyOne() throws Exception {
        var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
        doAnswer(call->{entered.countDown();assertTrue(release.await(10,TimeUnit.SECONDS));return null;}).when(audit).logForTenant(anyLong(),anyString(),anyString(),anyString(),anyString());
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var first=pool.submit(()->postDraft(true,"write","race",body(true)).andReturn());
            assertTrue(entered.await(10,TimeUnit.SECONDS));
            try { postDraft(true,"write","race",body(true)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_IN_PROGRESS")); }
            finally { release.countDown(); }
            assertEquals(201,first.get(10,TimeUnit.SECONDS).getResponse().getStatus());
        }
        postDraft(true,"write","race",body(true)).andExpect(status().isCreated());assertEquals(1,invoices.countByTenantId(tenant.getId()));
    }
    @Test void parallelDistinctWritesHaveUniqueNumbersWithConflictRetry() throws Exception {
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks=new ArrayList<Future<String>>();
            for(int i=0;i<8;i++) { final String key="parallel-"+i; tasks.add(pool.submit(()->{
                for(int attempt=0;attempt<100;attempt++) {
                    var response=postDraft(true,"write",key,body(true)).andReturn().getResponse();
                    if(response.getStatus()==409) { Thread.sleep(10);continue; }
                    assertEquals(201,response.getStatus(),response.getContentAsString());return mapper.readTree(response.getContentAsString()).at("/data/number").asText();
                }throw new AssertionError("Write did not complete");
            })); }
            var numbers=new HashSet<String>();for(var task:tasks) numbers.add(task.get(20,TimeUnit.SECONDS));assertEquals(8,numbers.size());
        }
    }
    @Test void apiCreatedDraftCurrentlyAppearsInUiAggregates_pendingDraftAggregateExclusion() throws Exception {
        postDraft(true,"write","aggregate-invoice",body(true)).andExpect(status().isCreated());
        postDraft(false,"write","aggregate-quote",body(false)).andExpect(status().isCreated());
        var ui=new TenantInvoicingServiceImpl(invoices,quotations,tenants,mock(TenantGovernance.class));
        var access=mock(TenantAccessService.class);when(access.getCurrentTenantId()).thenReturn(tenant.getId());
        var controller=new com.mbhoni_creative.admincontroller.TenantInvoiceViewController(ui,mock(TenantCustomizationService.class),
                mock(InvoiceDocumentService.class),mock(TenantSecurityService.class),access,mock(TenantService.class),mock(BillingAccountRepository.class));
        var model=new org.springframework.ui.ExtendedModelMap();controller.listInvoices(model);controller.listQuotations(model);
        for(String name:List.of("totalRevenue","totalUnpaid","totalQuoted")) assertEquals(new BigDecimal("0.03"),model.get(name));
        assertEquals(new BigDecimal("0.00"),model.get("totalPaid"));
    }
    @Test void ownerExportIncludesApiDraftStatusAndLinesWithoutOtherTenantData() throws Exception {
        for(boolean invoice:List.of(true,false)) {
            postDraft(invoice,"write","export-"+invoice,body(invoice)).andExpect(status().isCreated());
            postDraft(invoice,"other","export-"+invoice,body(invoice)).andExpect(status().isCreated());
        }
        var security=mock(TenantSecurityService.class);when(security.getCurrentTenantId()).thenReturn(tenant.getId());
        var subscriptions=mock(TenantSubscriptionRepository.class);when(subscriptions.findByTenantId(tenant.getId())).thenReturn(Optional.of(new TenantSubscription()));
        var export=new com.mbhoni_creative.billing.TenantExportController(mock(TenantGovernance.class),security,subscriptions,lifecycle,
                Clock.systemUTC(),jdbc,mapper,audit,new com.mbhoni_creative.billing.TenantDataScope(jdbc));
        var output=new java.io.ByteArrayOutputStream();export.export().getBody().writeTo(output);
        var json=mapper.readTree(output.toByteArray());
        for(String kind:List.of("invoice","quotation")) {
            var documents=json.get("tenant_"+kind+"s");assertEquals(1,documents.size());
            // JDBC column labels are upper case in H2, lower case in MySQL.
            var d=documents.get(0);assertEquals("DRAFT",d.has("status")?d.get("status").asText():d.get("STATUS").asText());
            assertEquals(1,json.get("tenant_"+kind+"_items").size());
        }
    }
    @Test void goldenRoundingMatchesActualUiServiceForBothDocuments() throws Exception {
        int[] counts={2,100,3};String[] quantities={"1.00","1.00","0.33"},prices={"0.03","0.03","0.05"},totals={"0.07","3.45","0.06"};
        var ui=new TenantInvoicingServiceImpl(invoices,quotations,tenants,mock(TenantGovernance.class));
        for(int c=0;c<counts.length;c++) for(boolean invoice:List.of(true,false)) {
            var tree=(com.fasterxml.jackson.databind.node.ObjectNode)mapper.readTree(body(invoice));var lines=mapper.createArrayNode();
            for(int i=0;i<counts[c];i++) lines.addObject().put("description","Line").put("quantity",quantities[c]).put("unitPrice",prices[c]);
            tree.set("items",lines);
            var response=postDraft(invoice,"write","golden-"+c+invoice,mapper.writeValueAsString(tree)).andExpect(status().isCreated()).andReturn().getResponse();
            var descriptions=Collections.nCopies(counts[c],"Line");var qty=Collections.nCopies(counts[c],new BigDecimal(quantities[c]));var price=Collections.nCopies(counts[c],new BigDecimal(prices[c]));
            BigDecimal expected=invoice?ui.saveInvoice(tenant.getId(),new TenantInvoice(),descriptions,qty,price).getTotalAmount()
                    :ui.saveQuotation(tenant.getId(),new TenantQuotation(),descriptions,qty,price).getTotalAmount();
            assertEquals(new BigDecimal(totals[c]),expected);
            assertEquals(expected.toPlainString(),mapper.readTree(response.getContentAsString()).at("/data/totalAmount").asText());
        }
    }
    @Test void apiCreatedDraftAdvancesUiNextNumber_pendingUiNumberingFix() throws Exception {
        for(boolean invoice:List.of(true,false)) {
            postDraft(invoice,"write","number-"+invoice,body(invoice)).andExpect(status().isCreated());
            var ui=new TenantInvoicingServiceImpl(invoices,quotations,tenants,mock(TenantGovernance.class));
            var user=new User();user.setUsername("ui-accountant");
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(new CustomUserPrincipal(user),null,List.of()));
            try {
                if(invoice) { var d=ui.saveInvoice(tenant.getId(),new TenantInvoice(),List.of("Line"),List.of(BigDecimal.ONE),List.of(BigDecimal.ONE));
                    assertTrue(d.getInvoiceNumber().endsWith("-002"));assertEquals("ui-accountant",d.getCreatedBy()); }
                else { var d=ui.saveQuotation(tenant.getId(),new TenantQuotation(),List.of("Line"),List.of(BigDecimal.ONE),List.of(BigDecimal.ONE));
                    assertTrue(d.getQuotationNumber().endsWith("-002"));assertEquals("ui-accountant",d.getCreatedBy()); }
            } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
        }
    }
}
