package com.mbhoni_creative.onboarding;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrialPlanInitializerTest {
    private final SubscriptionPlanRepository plans = mock(SubscriptionPlanRepository.class);
    private final PlatformModuleRepository modules = mock(PlatformModuleRepository.class);
    private final SubscriptionPlanModuleRepository mappings = mock(SubscriptionPlanModuleRepository.class);
    private final TrialPlanInitializer initializer = new TrialPlanInitializer(plans, modules, mappings);

    @Test void newTrialHasRequiredStorageAllowanceBeforeSave() {
        when(modules.findByCode(anyString())).thenAnswer(invocation -> {
            PlatformModule module = new PlatformModule();
            module.setCode(invocation.getArgument(0));
            return Optional.of(module);
        });
        when(plans.save(any())).thenAnswer(invocation -> {
            SubscriptionPlan plan = invocation.getArgument(0);
            assertEquals(Integer.valueOf(2000), plan.getMaxStorageMb());
            return plan;
        });

        initializer.run();

        ArgumentCaptor<SubscriptionPlan> saved = ArgumentCaptor.forClass(SubscriptionPlan.class);
        verify(plans).save(saved.capture());
        assertEquals(TrialPlanInitializer.CODE, saved.getValue().getCode());
        assertEquals(Integer.valueOf(5), saved.getValue().getMaxUsers());
        verify(mappings, times(5)).save(any());
    }

    @Test void restartPreservesExistingPlanConfiguration() {
        SubscriptionPlan existing = new SubscriptionPlan();
        existing.setMaxStorageMb(3000);
        when(plans.findByCode(TrialPlanInitializer.CODE)).thenReturn(Optional.of(existing));

        initializer.run();

        verify(plans, never()).save(any());
        assertEquals(Integer.valueOf(3000), existing.getMaxStorageMb());
        verifyNoInteractions(modules, mappings);
    }
}
