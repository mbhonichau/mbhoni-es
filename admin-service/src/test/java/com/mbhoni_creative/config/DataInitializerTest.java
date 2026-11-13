package com.mbhoni_creative.config;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataInitializerTest {
    @Test void repeatedStartupReusesExistingPlanCodesAndPreservesCustomPricing() {
        var roles = mock(RoleRepository.class);
        var permissions = mock(PermissionRepository.class);
        var modules = mock(PlatformModuleRepository.class);
        var tenants = mock(TenantRepository.class);
        var plans = mock(SubscriptionPlanRepository.class);
        when(roles.save(any())).thenAnswer(i -> i.getArgument(0));
        when(permissions.save(any())).thenAnswer(i -> i.getArgument(0));
        when(modules.save(any())).thenAnswer(i -> i.getArgument(0));
        when(tenants.save(any())).thenAnswer(i -> i.getArgument(0));
        Map<String, SubscriptionPlan> stored = new HashMap<>();
        SubscriptionPlan existing = new SubscriptionPlan();
        existing.setCode("PRO"); existing.setMonthlyPrice(new BigDecimal("175.00"));
        stored.put("PRO", existing);
        when(plans.findByCode(anyString())).thenAnswer(i -> Optional.ofNullable(stored.get(i.getArgument(0))));
        when(plans.save(any())).thenAnswer(i -> {
            SubscriptionPlan plan = i.getArgument(0);
            assertFalse(stored.containsKey(plan.getCode()), "Startup must not insert an existing unique plan code");
            stored.put(plan.getCode(), plan);
            return plan;
        });
        var initializer = new DataInitializer(mock(UserRepository.class), roles, tenants, permissions,
                modules, plans, mock(SubscriptionPlanModuleRepository.class),
                mock(TenantSubscriptionRepository.class), mock(TenantModuleRepository.class),
                mock(IndustryProfileRepository.class), mock(PasswordEncoder.class), mock(BootstrapAccounts.class));
        initializer.run();
        initializer.run();
        assertEquals(3, stored.size());
        assertSame(existing, stored.get("PRO"));
        assertEquals(new BigDecimal("175.00"), existing.getMonthlyPrice());
        verify(plans, times(2)).save(any()); // Only the missing STARTER and ENTERPRISE plans.
    }
}
