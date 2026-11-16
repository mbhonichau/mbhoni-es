package com.mbhoni_creative.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbhoni_creative.adminentity.SubscriptionStatus;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.adminservice.AuditLogService;
import com.mbhoni_creative.config.*;
import java.time.Clock;
import java.util.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class TenantExportController {
    private final TenantGovernance governance;
    private final TenantSecurityService security;
    private final TenantSubscriptionRepository subscriptions;
    private final SubscriptionLifecycle lifecycle;
    private final Clock clock;
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final AuditLogService audit;
    private final TenantDataScope scopes;
    public TenantExportController(TenantGovernance governance, TenantSecurityService security, TenantSubscriptionRepository subscriptions,
            SubscriptionLifecycle lifecycle, Clock clock, JdbcTemplate jdbc, ObjectMapper mapper, AuditLogService audit, TenantDataScope scopes) {
        this.governance = governance; this.security = security; this.subscriptions = subscriptions; this.lifecycle = lifecycle;
        this.clock = clock; this.jdbc = jdbc; this.mapper = mapper; this.audit = audit; this.scopes = scopes;
    }
    @GetMapping("/account/export")
    public ResponseEntity<StreamingResponseBody> export() {
        Long tenantId = security.getCurrentTenantId(); governance.requireOwner(tenantId);
        var state = lifecycle.accessState(subscriptions.findByTenantId(tenantId).orElseThrow());
        if (state.status() == SubscriptionStatus.CANCELLED && (state.retentionEndsAt() == null || !clock.instant().isBefore(state.retentionEndsAt())))
            throw new AccessDeniedException("Export retention has expired");
        if (state.status() == SubscriptionStatus.SUSPENDED) throw new AccessDeniedException("Suspended tenants have billing-only access");
        audit.logForTenant(tenantId, "TENANT_EXPORT_REQUESTED", "TENANT", String.valueOf(tenantId), "Owner requested retained business data");
        // Scope is captured before async streaming; never derive tenancy on the worker thread.
        StreamingResponseBody body = output -> {
            try (var json = mapper.getFactory().createGenerator(output)) {
                json.setCodec(mapper); json.writeStartObject(); json.writeNumberField("formatVersion", 1); json.writeNumberField("tenantId", tenantId);
                json.writeStringField("excluded", "Authentication secrets, API keys, external files and platform-wide configuration");
                for (var query : exportQueries(scopes.inspect()).entrySet()) {
                    json.writeFieldName(query.getKey()); json.writeObject(jdbc.queryForList(query.getValue().sql(), query.getValue().scope().arguments(tenantId)));
                }
                json.writeEndObject();
            }
        };
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tenant-" + tenantId + "-export.json").body(body);
    }
    record ExportQuery(String sql, TenantDataScope.Scope scope) {}
    static Map<String, ExportQuery> exportQueries(TenantDataScope.Graph graph) {
        Map<String, ExportQuery> queries = new LinkedHashMap<>();
        // An explicit allowlist prevents future secret-bearing tables from silently entering exports.
        for (String table : List.of("employees", "organization_units", "organization_memberships", "contracts", "service_catalog", "service_catalogs",
                "invoices", "billing_accounts", "tenant_invoices", "tenant_quotations", "business_expenses", "payslips", "tenant_content", "tenant_metrics",
                "tenant_onboarding_fields", "tenant_settings", "tenant_modules", "tenant_customizations", "tenant_quotas", "lookup_categories",
                "field_definitions", "audit_logs", "users", "roles", "employee_field_values", "contract_milestones", "service_slas", "payments",
                "tenant_invoice_items", "tenant_quotation_items", "lookup_codes", "user_roles", "role_permissions")) {
            TenantDataScope.Scope scope = graph.scopes().get(table);
            if (scope == null) continue; // Absent legacy tables and platform-owned tables are not tenant data.
            String columns = table.equals("users") ? "id, username, email, first_name, last_name, active, tenant_id" : "*";
            queries.put(table, new ExportQuery("SELECT " + columns + " FROM " + graph.tables().get(table).name() + " WHERE " + scope.predicate(), scope));
        }
        return queries;
    }
}