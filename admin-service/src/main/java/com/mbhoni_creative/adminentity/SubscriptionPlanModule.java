package com.mbhoni_creative.adminentity;

import jakarta.persistence.*;

@Entity
@Table(
    name = "subscription_plan_modules",
    uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "module_id"})
)
public class SubscriptionPlanModule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private PlatformModule module;

    private boolean allowed = true;

    public Long getId() { return id; }

    public SubscriptionPlan getPlan() { return plan; }
    public void setPlan(SubscriptionPlan plan) { this.plan = plan; }

    public PlatformModule getModule() { return module; }
    public void setModule(PlatformModule module) { this.module = module; }

    public boolean isAllowed() { return allowed; }
    public void setAllowed(boolean allowed) { this.allowed = allowed; }
}