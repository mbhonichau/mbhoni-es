package com.mbhoni_creative.api;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.ModuleService;
import com.mbhoni_creative.api.dto.ApiDtos;
import com.mbhoni_creative.api.mapper.ApiTimestamp;
import com.mbhoni_creative.api.service.ApiReadService;
import com.mbhoni_creative.billing.SubscriptionLifecycle;
import com.mbhoni_creative.config.ApiKeyPrincipal;
import com.mbhoni_creative.security.TenantContext;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiReadServiceTest {
    private final TenantRepository tenants = mock(TenantRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final TenantSubscriptionRepository subscriptions = mock(TenantSubscriptionRepository.class);
    private final TenantModuleRepository assignments = mock(TenantModuleRepository.class);
    private final SubscriptionPlanModuleRepository planModules = mock(SubscriptionPlanModuleRepository.class);
    private final ModuleService modules = mock(ModuleService.class);
    private final SubscriptionLifecycle lifecycle = mock(SubscriptionLifecycle.class);
    private final ApiReadService service = new ApiReadService(tenants, users, mock(TenantSettingsRepository.class),
            mock(TenantCustomizationRepository.class), mock(TenantQuotaRepository.class), subscriptions, assignments, planModules, modules, lifecycle);
    private final LocalDateTime old = LocalDateTime.of(2026, 1, 1, 0, 0);
    private Tenant tenant;
    private TenantSubscription subscription;
    private SubscriptionPlan plan;
    @BeforeEach void setup() {
        tenant = new Tenant(); tenant.setId(1L);
        var key = new TenantApiKey(); key.setTenant(tenant);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new ApiKeyPrincipal(key), null, List.of()));
        TenantContext.setTenantId(1L);
        when(tenants.findById(1L)).thenReturn(Optional.of(tenant));
        plan = new SubscriptionPlan(); plan.setCode("STANDARD");
        subscription = new TenantSubscription(); subscription.setTenant(tenant); subscription.setPlan(plan);
        when(subscriptions.findByTenantId(1L)).thenReturn(Optional.of(subscription));
        when(lifecycle.accessState(subscription)).thenReturn(new SubscriptionLifecycle.AccessState(SubscriptionStatus.ACTIVE, null, null));
        stamp(plan, old); stamp(subscription, old);
    }
    @AfterEach void clear() { TenantContext.clear(); SecurityContextHolder.clearContext(); }
    private void stamp(BaseEntity entity, LocalDateTime time) { ReflectionTestUtils.setField(entity, "updatedAt", time); }
    private TenantModule assignment(String code, LocalDateTime time) {
        PlatformModule module = new PlatformModule(); module.setCode(code); module.setName(code); stamp(module, old);
        TenantModule assignment = new TenantModule(); assignment.setTenant(tenant); assignment.setModule(module); stamp(assignment, time); return assignment;
    }
    @Test void moduleTimestampIncludesMappingAndPlanAndPagesAfterFiltering() {
        var a = assignment("A", old); var b = assignment("B", old);
        var mapping = new SubscriptionPlanModule(); mapping.setPlan(plan); mapping.setModule(b.getModule()); stamp(mapping, old.plusDays(3));
        when(assignments.findByTenantOrderByModuleCategoryAscModuleNameAsc(tenant)).thenReturn(List.of(b, a));
        when(planModules.findByPlanAndAllowedTrue(plan)).thenReturn(List.of(mapping));
        when(planModules.findByPlanAndModule(plan, b.getModule())).thenReturn(Optional.of(mapping));
        when(modules.isModuleEnabled(1L, "B")).thenReturn(true);
        var result = service.modules(0, 1, old.plusDays(1));
        assertEquals(1, result.page().totalElements()); assertEquals("B", result.data().getFirst().code());
        assertEquals(old.plusDays(3), result.data().getFirst().updatedAt()); assertTrue(result.data().getFirst().enabled());
        stamp(plan, old.plusDays(4));
        var both = service.modules(0, 1, old.plusDays(3));
        assertEquals(2, both.page().totalElements()); assertEquals("A", both.data().getFirst().code());
        assertEquals("B", service.modules(1, 1, old.plusDays(3)).data().getFirst().code());
        assertTrue(service.modules(Integer.MAX_VALUE, 100, null).data().isEmpty());
    }
    @Test void missingTimestampIsIncludedRatherThanLostAndNoWritesOccur() {
        var assignment = assignment("A", null);
        when(assignments.findByTenantOrderByModuleCategoryAscModuleNameAsc(tenant)).thenReturn(List.of(assignment));
        var result = service.modules(0, 25, old.plusYears(1));
        assertEquals(1, result.data().size()); assertNull(result.data().getFirst().updatedAt());
        verify(assignments, never()).save(any()); verify(subscriptions, never()).save(any());
    }
    @Test void equalityBoundaryAndMaximumPageSizeAreExplicit() {
        var assignment = assignment("A", old);
        when(assignments.findByTenantOrderByModuleCategoryAscModuleNameAsc(tenant)).thenReturn(List.of(assignment));
        assertTrue(service.modules(0, 100, old).data().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.modules(-1, 25, null));
        assertThrows(IllegalArgumentException.class, () -> service.modules(0, 101, null));
    }
    @Test void accessChecksUseExistingAuthorityDerivationAndFailClosedForInactiveUsers() {
        User user = new User(); user.setId(2L); user.setTenant(tenant);
        var role = new Role(); role.setName("ROLE_READER"); role.setTenant(tenant);
        var permission = new Permission(); permission.setName("USER_VIEW"); role.getPermissions().add(permission); user.getRoles().add(role);
        when(users.findByIdAndTenantId(2L, 1L)).thenReturn(Optional.of(user));
        assertTrue(service.check(new ApiDtos.AccessCheck(2L, "USER_VIEW")).allowed());
        assertFalse(service.check(new ApiDtos.AccessCheck(2L, "USER_DELETE")).allowed());
        user.setActive(false); assertFalse(service.check(new ApiDtos.AccessCheck(2L, "USER_VIEW")).allowed());
        verify(users, never()).findById(any());
    }
    @Test void mismatchedContextAndSuspendedTenantCannotUseApi() {
        TenantContext.setTenantId(2L);
        assertThrows(org.springframework.security.access.AccessDeniedException.class, service::profile);
        TenantContext.setTenantId(1L);
        when(lifecycle.accessState(subscription)).thenReturn(new SubscriptionLifecycle.AccessState(SubscriptionStatus.SUSPENDED, null, null));
        assertThrows(org.springframework.security.access.AccessDeniedException.class, service::profile);
    }
    @Test void offsetTimestampsConvertToServerZoneAndLocalTimestampsRemainLocal() {
        assertEquals(old, ApiTimestamp.parse("2026-01-01T00:00:00"));
        assertEquals(Instant.parse("2026-01-01T00:00:00Z").atZone(ZoneId.systemDefault()).toLocalDateTime(), ApiTimestamp.parse("2026-01-01T00:00:00Z"));
        assertThrows(java.time.format.DateTimeParseException.class, () -> ApiTimestamp.parse("yesterday"));
    }
}
