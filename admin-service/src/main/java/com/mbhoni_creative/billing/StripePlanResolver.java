package com.mbhoni_creative.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.mbhoni_creative.adminentity.SubscriptionPlan;
import com.mbhoni_creative.adminrepository.SubscriptionPlanRepository;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StripePlanResolver {
    private final SubscriptionPlanRepository plans;
    private final Environment environment;
    public StripePlanResolver(SubscriptionPlanRepository plans, Environment environment) {
        this.plans = plans; this.environment = environment;
    }
    public SubscriptionPlan resolve(JsonNode subscription) {
        JsonNode items = subscription.path("items").path("data");
        if (!items.isArray() || items.isEmpty() || subscription.path("items").path("has_more").asBoolean())
            throw new IllegalArgumentException("Complete subscription price components are required");
        SubscriptionPlan resolved = null;
        for (JsonNode item : items) {
        JsonNode priceNode = item.path("price");
        if (priceNode.has("currency") && !"usd".equals(priceNode.path("currency").asText()))
            throw new IllegalArgumentException("Platform billing supports USD only");
        String price = StripeGateway.id(priceNode);
        if (!price.matches("price_[A-Za-z0-9]+")) throw new IllegalArgumentException("Stripe price ID required");
        String code = environment.getProperty("app.stripe.price-plans." + price);
        if (code == null) throw new IllegalArgumentException("Stripe price has no configured local plan mapping");
        SubscriptionPlan plan = plans.findByCode(code).filter(SubscriptionPlan::isActive)
                .orElseThrow(() -> new IllegalArgumentException("Mapped subscription plan is unavailable"));
        if ("TRIAL_STARTER".equals(plan.getCode())) throw new IllegalArgumentException("Trial plan is not a paid conversion target");
        if (resolved != null && !resolved.getCode().equals(plan.getCode()))
            throw new IllegalArgumentException("All Stripe components must map to the same local plan");
        resolved = plan;
        }
        return resolved;
    }
}
