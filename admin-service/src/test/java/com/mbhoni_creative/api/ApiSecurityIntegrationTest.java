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

@SpringBootTest(classes = ApiSecurityIntegrationTest.App.class, properties = {
        "spring.cloud.config.enabled=false", "eureka.client.enabled=false", "app.stripe.webhook-secret=whsec_test",
        "spring.main.allow-bean-definition-overriding=false"})
@AutoConfigureMockMvc
class ApiSecurityIntegrationTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class})
    @Import({SecurityConfig.class, ApiSecurityConfig.class, ApiOpenApiConfig.class, ApiCredentialService.class,
            ApiKeyAuthenticationFilter.class, TenantContextFilter.class, TenantSecurityService.class, TenantAccessService.class,
            ApiController.class, ApiReadService.class, ApiExceptionHandler.class, ApiDocsController.class, ApiNotFoundController.class,
            EmployeeApiController.class, UserProfileApiController.class, IntegrationApiController.class, GlobalExceptionHandler.class,
            StripeWebhookController.class, StripeWebhookService.class, StripeSignatureVerifier.class, BillingApiErrors.class})
    static class App {
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
    @Test void unauthenticatedV1IsProblemJsonAndIgnoresExistingUiSession() throws Exception {
        stateless(mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("code").value("UNAUTHENTICATED")).andExpect(header().doesNotExist("Location")).andReturn());
        var session = new org.springframework.mock.web.MockHttpSession();
        var user = new User(); user.setId(2L); user.setTenant(tenant);
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(new CustomUserPrincipal(user), null, List.of());
        var security = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext(); security.setAuthentication(auth);
        session.setAttribute("SPRING_SECURITY_CONTEXT", security);
        mvc.perform(get("/api/v1/users/me").session(session)).andExpect(status().isUnauthorized());
    }
    @Test void invalidKeyAndBearerOnlyAre401AndUnknownGrantsAre403() throws Exception {
        for (String key : List.of("invalid", "unknown-permission")) {
            var result = mvc.perform(get("/api/v1/users/me").header("X-API-Key", key))
                    .andExpect(status().is(key.equals("invalid") ? 401 : 403)).andExpect(jsonPath("code").exists()).andReturn();
            stateless(result);
        }
        stateless(mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer unsupported"))
                .andExpect(status().isUnauthorized()).andReturn());
    }
    @Test void keyIdentityUsesCredentialTenantAndExcludesSecrets() throws Exception {
        var result = mvc.perform(get("/api/v1/users/me").header("X-API-Key", "valid").header("X-Tenant-ID", "999").param("tenantId", "999"))
                .andExpect(status().isOk()).andExpect(jsonPath("data.tenantId").value(1))
                .andExpect(jsonPath("data.type").value("API_KEY")).andExpect(jsonPath("data.keyHash").doesNotExist())
                .andExpect(jsonPath("page").doesNotExist()).andReturn(); stateless(result);
        assertFalse(result.getResponse().getContentAsString().contains("password"));
        verify(tenants, never()).findById(999L);
    }
    @Test void userLookupIsTenantScopedAndCrossTenantIs404() throws Exception {
        mvc.perform(get("/api/v1/users/99").header("X-API-Key", "valid"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("code").value("NOT_FOUND"));
        verify(users).findByIdAndTenantId(99L, 1L); verify(users, never()).findById(any());
        mvc.perform(get("/api/v1/users/99").header("X-API-Key", "tenant-only"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("code").value("FORBIDDEN"));
    }
    @Test void postCheckNeedsScopeButNotCsrfAndNeverCreatesSession() throws Exception {
        stateless(mvc.perform(post("/api/v1/access/check").header("X-API-Key", "valid").contentType("application/json")
                .content("{\"userId\":99,\"permission\":\"USER_VIEW\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("data.allowed").value(false)).andReturn());
        mvc.perform(post("/api/v1/access/check").header("X-API-Key", "valid").contentType("application/json").content("{}"))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("code").value("VALIDATION_FAILED"));
        mvc.perform(post("/api/v1/access/check").header("X-API-Key", "valid").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest());
    }
    @Test void defaultsAndQuotaGetDoNotWriteRows() throws Exception {
        for (String path : List.of("tenant", "tenant/branding", "tenant/quotas"))
            stateless(mvc.perform(get("/api/v1/" + path).header("X-API-Key", "valid")).andExpect(status().isOk()).andReturn());
        verify(settings, never()).save(any()); verify(branding, never()).save(any()); verify(quotas, never()).save(any());
        verify(tenants, never()).save(any());
    }
    @Test void invalidPaginationUnknownPathAndInternalFailureAreProblems() throws Exception {
        mvc.perform(get("/api/v1/tenant/modules").param("size", "101").header("X-API-Key", "valid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("code").value("INVALID_REQUEST"));
        mvc.perform(get("/api/v1/does-not-exist").header("X-API-Key", "valid"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("code").value("NOT_FOUND"));
        when(tenants.findById(1L)).thenThrow(new IllegalStateException("password_hash SECRET"));
        var result = mvc.perform(get("/api/v1/tenant").header("X-API-Key", "valid"))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("code").value("INTERNAL_ERROR")).andReturn();
        assertFalse(result.getResponse().getContentAsString().contains("SECRET")); stateless(result);
    }
    @Test void legacyRoutesMatchOriginalChainWithAndWithoutApiKeyAndCsrf() throws Exception {
        MockMvc baseline = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity(new FilterChainProxy(List.of(legacyChain))))
                .addFilters(legacyKeyFilter, legacyTenantFilter).build();
        for (String path : List.of("/api/employees?tenantId=1", "/api/user/profile", "/api/integrations/me")) {
            for (boolean credential : List.of(false, true)) {
                for (boolean write : List.of(false, true)) {
                    var oldRequest = write ? post(path) : get(path); var newRequest = write ? post(path) : get(path);
                    if (credential) { oldRequest.header("X-API-Key", "valid"); newRequest.header("X-API-Key", "valid"); }
                    var before = baseline.perform(oldRequest).andReturn().getResponse();
                    var after = mvc.perform(newRequest).andReturn().getResponse();
                    assertEquals(before.getStatus(), after.getStatus(), path + " credential=" + credential + " write=" + write);
                    assertEquals(before.getRedirectedUrl(), after.getRedirectedUrl()); assertEquals(before.getForwardedUrl(), after.getForwardedUrl());
                }
            }
        }
        mvc.perform(get("/api/employees").param("tenantId", "1").header("X-API-Key", "valid")).andExpect(status().isOk());
        mvc.perform(post("/api/employees").param("tenantId", "1").header("X-API-Key", "valid").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        // An existing mutation still accepts its existing CSRF/session path; compare against original chain.
        var before = baseline.perform(put("/api/user/profile").with(csrf()).header("X-API-Key", "valid").contentType("application/json").content("{}"))
                .andReturn().getResponse();
        var after = mvc.perform(put("/api/user/profile").with(csrf()).header("X-API-Key", "valid").contentType("application/json").content("{}"))
                .andReturn().getResponse(); assertEquals(before.getStatus(), after.getStatus());
    }
    @Test void stripeWebhookStillVerifiesRawSignatureWithoutCsrfOrApiKey() throws Exception {
        byte[] body = "{\"id\":\"evt_test\",\"type\":\"unhandled.event\"}".getBytes(StandardCharsets.UTF_8);
        String timestamp = "" + Instant.parse("2026-11-13T12:00:00Z").getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec("whsec_test".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
        String signature = "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(mac.doFinal(body));
        mvc.perform(post("/api/webhooks/stripe").contentType("application/json").content(body).header("Stripe-Signature", signature)).andExpect(status().isOk());
        mvc.perform(post("/api/webhooks/stripe").contentType("application/json").content(body).header("Stripe-Signature", "bad"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(stripe);
    }
    @Test void documentationExistsOnlyAtApprovedRoutes() throws Exception {
        stateless(mvc.perform(get("/api/docs")).andExpect(status().isOk()).andReturn());
        var result = mvc.perform(get("/api/openapi.json").header("X-API-Key", "valid"))
                .andExpect(status().isOk()).andExpect(jsonPath("openapi").exists()).andExpect(jsonPath("paths['/api/v1/users/me']").exists()).andReturn();
        stateless(result); assertFalse(result.getResponse().getContentAsString().contains("/api/employees"));
        var mapping = context.getBean("requestMappingHandlerMapping", org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping.class);
        var paths = mapping.getHandlerMethods().keySet().stream().flatMap(m -> m.getPatternValues().stream()).toList();
        for (String path : List.of("/v3/api-docs", "/v3/api-docs.yaml", "/swagger-ui.html", "/swagger-ui/index.html", "/api/openapi.json.yaml")) {
            assertFalse(paths.contains(path), path);
            mvc.perform(get(path).header("X-API-Key", "valid")).andExpect(status().isNotFound());
        }
    }
}
