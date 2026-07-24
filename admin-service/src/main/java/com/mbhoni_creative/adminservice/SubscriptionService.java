package com.mbhoni_creative.adminservice;

import java.time.LocalDate;
import java.util.List;

import com.mbhoni_creative.adminentity.BillingCycle;
import com.mbhoni_creative.adminentity.SubscriptionPlan;
import com.mbhoni_creative.adminentity.SubscriptionStatus;
import com.mbhoni_creative.adminentity.TenantSubscription;

public interface SubscriptionService {

    List<SubscriptionPlan> getAllPlans();

    List<SubscriptionPlan> getActivePlans();

    SubscriptionPlan getPlanById(Long id);

    SubscriptionPlan savePlan(SubscriptionPlan plan);

    SubscriptionPlan updatePlan(Long id, SubscriptionPlan plan);

    void deactivatePlan(Long id);

    List<TenantSubscription> getAllTenantSubscriptions();

    TenantSubscription assignTenantSubscription(
            Long tenantId,
            Long planId,
            SubscriptionStatus status,
            BillingCycle billingCycle,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate trialEndsAt,
            boolean autoRenew
    );
}