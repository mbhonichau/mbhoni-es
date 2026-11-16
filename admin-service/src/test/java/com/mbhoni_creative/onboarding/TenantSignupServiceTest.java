package com.mbhoni_creative.onboarding;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantSignupServiceTest {
    private final TenantRepository tenants = mock(TenantRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final SubscriptionPlanRepository plans = mock(SubscriptionPlanRepository.class);
    private final TenantSubscriptionRepository subscriptions = mock(TenantSubscriptionRepository.class);
    private final TenantSettingsRepository settings = mock(TenantSettingsRepository.class);
    private final TenantCustomizationRepository customizations = mock(TenantCustomizationRepository.class);
    private final BillingAccountRepository accounts = mock(BillingAccountRepository.class);
    private final SubscriptionPlanModuleRepository planModules = mock(SubscriptionPlanModuleRepository.class);
    private final TenantModuleRepository tenantModules = mock(TenantModuleRepository.class);
    private final AuditLogRepository audit = mock(AuditLogRepository.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-26T12:00:00Z"), ZoneOffset.UTC);
    private final TenantSignupService service = new TenantSignupService(tenants, users, roles, plans, subscriptions,
            settings, customizations, accounts, planModules, tenantModules, audit, passwords, clock, 14);
    @BeforeEach void setup() {
        SubscriptionPlan trial = new SubscriptionPlan(); trial.setMaxUsers(5); trial.setCode("TRIAL_STARTER"); trial.setActive(true);
        when(plans.findByCode("TRIAL_STARTER")).thenReturn(Optional.of(trial));
        when(roles.findByNameAndTenantIsNull(anyString())).thenReturn(Optional.of(new Role()));
        when(roles.save(any())).thenAnswer(i -> i.getArgument(0));
        when(tenants.save(any())).thenAnswer(i -> { Tenant t = i.getArgument(0); t.setId(42L); return t; });
        when(users.save(any())).thenAnswer(i -> { User u = i.getArgument(0); u.setId(7L); return u; });
        when(passwords.encode(anyString())).thenReturn("hash");
    }
    @Test void signupCreatesTrialOwnerAndDefaultRecordsWithoutSelectingExistingTenant() {
        User owner = service.signup("Acme", "creator", "Creator@example.com", "a-long-password");
        assertEquals(7L, owner.getTenant().getOwnerUserId());
        assertEquals("TRIAL", owner.getTenant().getStatus()); assertFalse(owner.isGlobalAdmin());
        assertEquals("creator@example.com", owner.getEmail()); assertEquals("ROLE_OWNER", owner.getRoles().iterator().next().getName());
        verify(subscriptions).save(argThat(s -> s.getStatus() == SubscriptionStatus.TRIAL
                && s.getPlan().getMaxUsers() == 5 && s.getTrialEndsAt().equals(LocalDate.of(2026, 10, 10)) && !s.isAutoRenew()));
        verify(settings).save(argThat(s -> !s.isSelfRegistrationAllowed()));
        verify(customizations).save(any()); verify(accounts).save(any()); verify(audit).save(any());
        verify(tenants, never()).findAll();
    }
    @Test void provisioningFailureEscapesTransactionalBoundary() throws Exception {
        when(accounts.save(any())).thenThrow(new IllegalStateException("database failure"));
        assertThrows(IllegalStateException.class, () -> service.signup("Acme", "creator", "creator@example.com", "a-long-password"));
        assertNotNull(TenantSignupService.class.getMethod("signup", String.class, String.class, String.class, String.class).getAnnotation(Transactional.class));
        verifyNoInteractions(audit); // Failure must not be reported as a completed signup.
    }
    @Test void duplicateAndInvalidSignupDoNotCreateTenant() {
        when(users.existsByEmail("creator@example.com")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> service.signup("Acme", "creator", "creator@example.com", "a-long-password"));
        assertThrows(IllegalArgumentException.class, () -> service.signup("Acme", "creator", "creator@example.com", "short"));
        verify(tenants, never()).save(any());
    }
    @Test void databaseWritesRollBackWhenProvisioningFails() {
        var source = new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:signup_rollback;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new org.springframework.jdbc.core.JdbcTemplate(source);
        jdbc.execute("CREATE TABLE provisioned_records(kind VARCHAR(30))");
        doAnswer(i -> {
            jdbc.update("INSERT INTO provisioned_records VALUES (?)", "tenant");
            Tenant tenant = i.getArgument(0); tenant.setId(42L); return tenant;
        }).when(tenants).save(any());
        doAnswer(i -> {
            jdbc.update("INSERT INTO provisioned_records VALUES (?)", "user");
            User user = i.getArgument(0); user.setId(7L); return user;
        }).when(users).save(any());
        when(accounts.save(any())).thenThrow(new IllegalStateException("provisioning failure"));
        var manager = new org.springframework.jdbc.datasource.DataSourceTransactionManager(source);
        var interceptor = new org.springframework.transaction.interceptor.TransactionInterceptor();
        interceptor.setTransactionManager(manager);
        interceptor.setTransactionAttributeSource(new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource());
        var factory = new org.springframework.aop.framework.ProxyFactory(service); factory.setProxyTargetClass(true); factory.addAdvice(interceptor);
        TenantSignupService proxy = (TenantSignupService) factory.getProxy();
        assertThrows(IllegalStateException.class, () -> proxy.signup("Acme", "creator", "creator@example.com", "a-long-password"));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM provisioned_records", Integer.class));
    }
}
