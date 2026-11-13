package com.mbhoni_creative.adminentity;

import java.time.LocalDate;

import jakarta.persistence.*;

@Entity
@Table(name = "tenant_subscriptions")
public class TenantSubscription extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate trialEndsAt;

    private boolean autoRenew = true;

    @Column(unique = true)
    private String stripeSubscriptionId;
    @Column(unique = true)
    private String stripeCustomerId;
    private java.time.Instant stateChangedAt;
    private java.time.Instant graceEndsAt;
    private java.time.Instant retentionEndsAt;

    @Column(columnDefinition = "TEXT")
    private String stripeItemsJson;
    private Boolean cancelAtPeriodEnd;
    private java.time.Instant currentPeriodEnd;
    private java.time.Instant providerSyncedAt;
    private String providerStatus;

    public String getStripeItemsJson() { return stripeItemsJson; }
    public void setStripeItemsJson(String value) { stripeItemsJson = value; }
    public Boolean getCancelAtPeriodEnd() { return cancelAtPeriodEnd; }
    public void setCancelAtPeriodEnd(Boolean value) { cancelAtPeriodEnd = value; }
    public java.time.Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(java.time.Instant value) { currentPeriodEnd = value; }
    public java.time.Instant getProviderSyncedAt() { return providerSyncedAt; }
    public void setProviderSyncedAt(java.time.Instant value) { providerSyncedAt = value; }
    public String getProviderStatus() { return providerStatus; }
    public void setProviderStatus(String value) { providerStatus = value; }

    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String value) { stripeSubscriptionId = value; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String value) { stripeCustomerId = value; }
    public java.time.Instant getStateChangedAt() { return stateChangedAt; }
    public void setStateChangedAt(java.time.Instant value) { stateChangedAt = value; }
    public java.time.Instant getGraceEndsAt() { return graceEndsAt; }
    public void setGraceEndsAt(java.time.Instant value) { graceEndsAt = value; }
    public java.time.Instant getRetentionEndsAt() { return retentionEndsAt; }
    public void setRetentionEndsAt(java.time.Instant value) { retentionEndsAt = value; }

    public Long getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public SubscriptionPlan getPlan() {
        return plan;
    }

    public void setPlan(SubscriptionPlan plan) {
        this.plan = plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }

    public BillingCycle getBillingCycle() {
        return billingCycle;
    }

    public void setBillingCycle(BillingCycle billingCycle) {
        this.billingCycle = billingCycle;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public LocalDate getTrialEndsAt() {
        return trialEndsAt;
    }

    public void setTrialEndsAt(LocalDate trialEndsAt) {
        this.trialEndsAt = trialEndsAt;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }
}
