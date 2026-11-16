package com.mbhoni_creative.onboarding;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(100)
public class TrialPlanInitializer implements CommandLineRunner {
    public static final String CODE = "TRIAL_STARTER";
    private final SubscriptionPlanRepository plans;
    private final PlatformModuleRepository modules;
    private final SubscriptionPlanModuleRepository mappings;
    public TrialPlanInitializer(SubscriptionPlanRepository plans, PlatformModuleRepository modules, SubscriptionPlanModuleRepository mappings) {
        this.plans = plans; this.modules = modules; this.mappings = mappings;
    }
    @Override @Transactional
    public void run(String... args) {
        if (plans.findByCode(CODE).isPresent()) return;
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setCode(CODE); plan.setName("14-day evaluation");
        plan.setDescription("Core features with a hard cap of 5 users; no card required to start.");
        plan.setMonthlyPrice(BigDecimal.ZERO); plan.setAnnualPrice(BigDecimal.ZERO);
        plan.setMaxUsers(5); plan.setApiAccessEnabled(true); plan.setBrandingEnabled(true); plan.setActive(true);
        plan.setMaxStorageMb(2000);
        plans.save(plan);
        for (String code : List.of("EMPLOYEE", "ORG", "CONTRACT", "MASTER_DATA", "SERVICE")) {
            PlatformModule module = modules.findByCode(code).orElseThrow(() -> new IllegalStateException("Missing core module " + code));
            SubscriptionPlanModule mapping = new SubscriptionPlanModule(); mapping.setPlan(plan); mapping.setModule(module);
            mapping.setAllowed(true); mappings.save(mapping);
        }
    }
}
