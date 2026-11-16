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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final TenantSubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final NotificationService notificationService;
    private final com.mbhoni_creative.config.TenantSecurityService tenantSecurityService;
    private final com.mbhoni_creative.billing.SubscriptionLifecycle lifecycle;
    private final com.mbhoni_creative.adminservice.AuditLogService audit;
    private final java.time.Clock clock;
    @PersistenceContext
    private EntityManager entityManager;

    public SubscriptionServiceImpl(
            SubscriptionPlanRepository planRepository,
            TenantSubscriptionRepository subscriptionRepository,
            TenantRepository tenantRepository,
            NotificationService notificationService,
            com.mbhoni_creative.config.TenantSecurityService tenantSecurityService,
            com.mbhoni_creative.billing.SubscriptionLifecycle lifecycle,
            com.mbhoni_creative.adminservice.AuditLogService audit, java.time.Clock clock) {

        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tenantRepository = tenantRepository;
        this.notificationService = notificationService;
        this.tenantSecurityService = tenantSecurityService;
        this.lifecycle = lifecycle; this.audit = audit; this.clock = clock;
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
        requirePlatformAdmin();
        if (plan.getId() != null) throw new IllegalArgumentException("New plans cannot supply an ID");

        if (planRepository.existsByCode(plan.getCode())) {
            throw new RuntimeException("Subscription plan code already exists");
        }

        plan.setActive(true);

        return planRepository.save(plan);
    }

    @Override
    @Transactional
    public SubscriptionPlan updatePlan(Long id, SubscriptionPlan source) {
        requirePlatformAdmin();

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
        requirePlatformAdmin();

        SubscriptionPlan plan = getPlanById(id);

        plan.setActive(false);

        planRepository.save(plan);
    }

    @Override
    @Transactional
    public void deletePlan(Long id) {

        requirePlatformAdmin();
        if (subscriptionRepository.existsByPlanId(id)) {
            throw new IllegalStateException("Reassign subscriptions before deleting a plan");
        }

        SubscriptionPlan plan = getPlanById(id);

        // Delete plan-module mappings
        entityManager.createNativeQuery("DELETE FROM subscription_plan_modules WHERE plan_id = :id")
                .setParameter("id", id).executeUpdate();

        // Delete plan
        planRepository.delete(plan);
    }

    @Override
    public List<TenantSubscription> getAllTenantSubscriptions() {
        if (tenantSecurityService.isGlobalAdmin()) {
            return subscriptionRepository.findAll();
        }
        Long tenantId = tenantSecurityService.getCurrentTenantId();
        if (tenantId == null) {
            return List.of();
        }
        return subscriptionRepository.findByTenantId(tenantId)
                .map(List::of)
                .orElseGet(List::of);
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
            boolean autoRenew, String reason) {

        requirePlatformAdmin();
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Override reason required");
        if (status == null || billingCycle == null || (startDate != null && endDate != null && endDate.isBefore(startDate))
                || (status == SubscriptionStatus.TRIAL && trialEndsAt == null)) {
            throw new IllegalArgumentException("Invalid subscription state or dates");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Subscription plan not found"));

        TenantSubscription subscription = subscriptionRepository.lockByTenantId(tenantId)
                .orElseGet(TenantSubscription::new);

        String oldPlanName = subscription.getPlan() != null ? subscription.getPlan().getName() : "None";

        subscription.setTenant(tenant);
        if (subscription.getId() != null) {
            lifecycle.transition(subscription, status, clock.instant(), false, "platform override: " + reason.trim());
        } else {
            if (status != SubscriptionStatus.TRIAL && status != SubscriptionStatus.ACTIVE) {
                throw new IllegalArgumentException("Initial subscription must be TRIAL or ACTIVE");
            }
            subscription.setStatus(status);
            subscription.setStateChangedAt(clock.instant());
            tenant.setStatus(status.name()); tenant.setActive(true);
        }
        subscription.setPlan(plan);
        subscription.setBillingCycle(billingCycle);
        subscription.setStartDate(startDate);
        subscription.setEndDate(endDate);
        subscription.setTrialEndsAt(trialEndsAt);
        subscription.setAutoRenew(autoRenew);

        TenantSubscription savedSubscription = subscriptionRepository.save(subscription);
        audit.logForTenant(tenantId, "SUBSCRIPTION_OVERRIDE", "TENANT", String.valueOf(tenantId), reason.trim());

        if (!oldPlanName.equals(plan.getName())) {
            notificationService.sendSubscriptionChangeAlert(tenant.getName(), oldPlanName, plan.getName());
        }

        return savedSubscription;
    }

    private void requirePlatformAdmin() {
        if (!tenantSecurityService.isGlobalAdmin()) throw new org.springframework.security.access.AccessDeniedException("Platform administrator required");
    }
}
