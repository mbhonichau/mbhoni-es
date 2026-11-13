package com.mbhoni_creative.billing;

import com.mbhoni_creative.adminentity.SubscriptionStatus;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantPurgeService {
    private final TenantSubscriptionRepository subscriptions;
    private final TenantDataScope scopes;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final EntityManager entities;
    public TenantPurgeService(TenantSubscriptionRepository subscriptions, TenantDataScope scopes, JdbcTemplate jdbc, Clock clock, EntityManager entities) {
        this.subscriptions = subscriptions; this.scopes = scopes; this.jdbc = jdbc; this.clock = clock; this.entities = entities;
    }
    @Transactional
    public boolean purgeExpired(Long tenantId) {
        var subscription = subscriptions.lockByTenantId(tenantId).orElse(null);
        if (subscription == null || subscription.getStatus() != SubscriptionStatus.CANCELLED || subscription.getRetentionEndsAt() == null
                || clock.instant().isBefore(subscription.getRetentionEndsAt())) return false;
        entities.flush();
        TenantDataScope.Graph graph = scopes.inspect();
        rejectForeignDependents(graph, tenantId);
        jdbc.update("UPDATE tenants SET owner_user_id = NULL WHERE id = ?", tenantId);
        for (String name : graph.deleteOrder()) {
            var table = graph.tables().get(name); var scope = graph.scopes().get(name);
            for (var key : table.keys()) if (key.parent().equalsIgnoreCase(table.name())) {
                if (!key.nullable()) throw new IllegalStateException("Non-null self reference prevents safe tenant purge");
                jdbc.update("UPDATE " + table.name() + " SET " + key.column() + " = NULL WHERE " + scope.predicate(), scope.arguments(tenantId));
            }
            jdbc.update("DELETE FROM " + table.name() + " WHERE " + scope.predicate(), scope.arguments(tenantId));
        }
        int removed = jdbc.update("DELETE FROM tenants WHERE id = ?", tenantId);
        if (removed != 1) throw new IllegalStateException("Tenant purge did not remove exactly one tenant");
        entities.clear();
        return true;
    }

    private void rejectForeignDependents(TenantDataScope.Graph graph, Long tenantId) {
        // FK cascades must not bypass our scope predicates, including inconsistent legacy rows.
        for (var table : graph.tables().values()) {
            var child = table.name().equalsIgnoreCase("tenants") ? new TenantDataScope.Scope("id = ?", 1)
                    : graph.scopes().get(table.name().toLowerCase(java.util.Locale.ROOT));
            for (var key : table.keys()) {
                var parent = key.parent().equalsIgnoreCase("tenants") ? new TenantDataScope.Scope("id = ?", 1)
                        : graph.scopes().get(key.parent().toLowerCase(java.util.Locale.ROOT));
                if (parent == null) continue;
                String predicate = key.column() + " IN (SELECT " + key.parentColumn() + " FROM " + key.parent()
                        + " WHERE " + parent.predicate() + ")";
                java.util.List<Object> arguments = new java.util.ArrayList<>(java.util.Arrays.asList(parent.arguments(tenantId)));
                if (child != null) {
                    predicate += " AND NOT COALESCE((" + child.predicate() + "), FALSE)";
                    arguments.addAll(java.util.Arrays.asList(child.arguments(tenantId)));
                }
                Integer foreign = jdbc.queryForObject("SELECT COUNT(*) FROM " + table.name() + " WHERE " + predicate, Integer.class, arguments.toArray());
                if (foreign == null || foreign != 0) throw new IllegalStateException("Cross-tenant dependency prevents safe purge: " + table.name());
            }
        }
    }
}
