package com.mbhoni_creative.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.mbhoni_creative.adminentity.BillingCycle;
import com.mbhoni_creative.adminentity.TenantSubscription;
import java.time.*;

/** Provider values only: no charge, tax or proration arithmetic. */
final class StripeSubscriptionSnapshot {
    private StripeSubscriptionSnapshot() {}
    static void apply(TenantSubscription local, JsonNode remote, Instant now) {
        JsonNode items = remote.path("items").path("data");
        BillingCycle cycle = null;
        Instant end = remote.path("current_period_end").canConvertToLong()
                ? Instant.ofEpochSecond(remote.path("current_period_end").asLong()) : null;
        for (JsonNode item : items) {
            JsonNode recurring = item.path("price").path("recurring");
            if (recurring.has("interval")) {
                BillingCycle itemCycle = switch (recurring.path("interval").asText()) {
                    case "month" -> BillingCycle.MONTHLY;
                    case "year" -> BillingCycle.ANNUAL;
                    default -> throw new IllegalArgumentException("Only monthly and annual billing is supported");
                };
                if (recurring.path("interval_count").asInt(1) != 1 || cycle != null && cycle != itemCycle)
                    throw new IllegalArgumentException("Mixed or multi-period billing requires explicit support");
                cycle = itemCycle;
            }
            if (item.path("current_period_end").canConvertToLong()) {
                Instant itemEnd = Instant.ofEpochSecond(item.path("current_period_end").asLong());
                if (end != null && !end.equals(itemEnd)) throw new IllegalArgumentException("Mixed billing periods are unsupported");
                end = itemEnd;
            }
        }
        if (cycle != null) local.setBillingCycle(cycle);
        local.setStripeItemsJson(items.toString());
        local.setCurrentPeriodEnd(end);
        local.setCancelAtPeriodEnd(remote.path("cancel_at_period_end").asBoolean());
        local.setProviderStatus(remote.path("status").asText());
        local.setProviderSyncedAt(now);
    }
}
