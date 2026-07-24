package com.mbhoni_creative.adminservice.impl;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.adminentity.BillingCycle;
import com.mbhoni_creative.adminentity.SubscriptionPlan;
import com.mbhoni_creative.adminentity.SubscriptionStatus;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantSubscription;
import com.mbhoni_creative.adminrepository.SubscriptionPlanRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.adminservice.SubscriptionService;
import com.mbhoni_creative.adminservice.NotificationService;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final NotificationService notificationService;

    public SubscriptionServiceImpl(
            SubscriptionPlanRepository planRepository,
            TenantSubscriptionRepository subscriptionRepository,
            TenantRepository tenantRepository,
            NotificationService notificationService) {

        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tenantRepository = tenantRepository;
        this.notificationService = notificationService;
    }

    @Override
    public List<SubscriptionPlan> getAllPlans() {
        return planRepository.findAll();
    }

    @Override
    public List<SubscriptionPlan> getActivePlans() {
        return planRepository.findByActiveTrueOrderByNameAsc();
    }

    @Override
    public SubscriptionPlan getPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));
    }

    @Override
    @Transactional
    public SubscriptionPlan savePlan(SubscriptionPlan plan) {

        if (planRepository.existsByCode(plan.getCode())) {
            throw new RuntimeException("Subscription plan code already exists");
        }

        plan.setActive(true);

        return planRepository.save(plan);
    }

    @Override
    @Transactional
    public SubscriptionPlan updatePlan(Long id, SubscriptionPlan source) {

        SubscriptionPlan plan = getPlanById(id);

        plan.setCode(source.getCode());
        plan.setName(source.getName());
        plan.setDescription(source.getDescription());
        plan.setMonthlyPrice(source.getMonthlyPrice());
        plan.setAnnualPrice(source.getAnnualPrice());
        plan.setMaxUsers(source.getMaxUsers());
        plan.setMaxStorageMb(source.getMaxStorageMb());
        plan.setApiAccessEnabled(source.isApiAccessEnabled());
        plan.setBrandingEnabled(source.isBrandingEnabled());
        plan.setCustomDomainEnabled(source.isCustomDomainEnabled());
        plan.setActive(source.isActive());

        return planRepository.save(plan);
    }

    @Override
    @Transactional
    public void deactivatePlan(Long id) {

        SubscriptionPlan plan = getPlanById(id);

        plan.setActive(false);

        planRepository.save(plan);
    }

    @Override
    public List<TenantSubscription> getAllTenantSubscriptions() {
        return subscriptionRepository.findAll();
    }

    @Override
    @Transactional
    public TenantSubscription assignTenantSubscription(
            Long tenantId,
            Long planId,
            SubscriptionStatus status,
            BillingCycle billingCycle,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate trialEndsAt,
            boolean autoRenew) {

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        TenantSubscription subscription = subscriptionRepository.findByTenant(tenant)
                .orElseGet(TenantSubscription::new);

        String oldPlanName = subscription.getPlan() != null ? subscription.getPlan().getName() : "None";

        subscription.setTenant(tenant);
        subscription.setPlan(plan);
        subscription.setStatus(status);
        subscription.setBillingCycle(billingCycle);
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setTrialEndsAt(trialEndsAt);
        subscription.setAutoRenew(autoRenew);

        TenantSubscription savedSubscription = subscriptionRepository.save(subscription);

        if (!oldPlanName.equals(plan.getName())) {
            notificationService.sendSubscriptionChangeAlert(tenant.getName(), oldPlanName, plan.getName());
        }

        return savedSubscription;
    }
}