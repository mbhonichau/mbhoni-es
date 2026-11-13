package com.mbhoni_creative.adminservice;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mbhoni_creative.admindto.UserDto;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.User;
import com.mbhoni_creative.adminrepository.RoleRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.adminrepository.UserRepository;
import com.mbhoni_creative.adminservice.impl.UserServiceImpl;
import com.mbhoni_creative.config.TenantSecurityService;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private TenantSubscriptionRepository tenantSubscriptionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TenantSecurityService tenantSecurityService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;
    @Mock private com.mbhoni_creative.adminrepository.TenantQuotaRepository quotaRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setName("Test Tenant");
        tenant.setActive(true);

        user = new User();
        user.setId(100L);
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setTenant(tenant);
        user.setActive(true);
    }

    @Test
    void testSaveUser_Success() {
        UserDto dto = new UserDto();
        dto.setUsername("newuser");
        dto.setEmail("newuser@example.com");
        dto.setPassword("Secret123!");
        dto.setTenantId(1L);

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(true);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.lockById(1L)).thenReturn(Optional.of(tenant));
        com.mbhoni_creative.adminentity.TenantSubscription subscription = new com.mbhoni_creative.adminentity.TenantSubscription();
        subscription.setPlan(new com.mbhoni_creative.adminentity.SubscriptionPlan());
        when(tenantSubscriptionRepository.findByTenant(tenant)).thenReturn(Optional.of(subscription));
        when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

        userService.saveUser(dto);

        verify(userRepository).save(any(User.class));
        verify(notificationService).sendWelcomeEmail("newuser", "newuser@example.com", "Test Tenant");
    }

    @Test
    void testSaveUser_DuplicateUsername_ThrowsException() {
        UserDto dto = new UserDto();
        dto.setUsername("testuser");
        dto.setEmail("new@example.com");

        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> userService.saveUser(dto));
        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void testGenerateTemporaryPassword_LengthAndFormat() {
        String tempPass = userService.generateTemporaryPassword();
        assertNotNull(tempPass);
        assertEquals(12, tempPass.length());
    }
    @Test void trialSeatCapBlocksSixthUserBeforePersistingOrSendingWelcome() {
        UserDto dto = new UserDto(); dto.setUsername("sixth"); dto.setEmail("sixth@example.com"); dto.setTenantId(1L);
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(true);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.lockById(1L)).thenReturn(Optional.of(tenant));
        var plan = new com.mbhoni_creative.adminentity.SubscriptionPlan(); plan.setMaxUsers(5);
        var subscription = new com.mbhoni_creative.adminentity.TenantSubscription(); subscription.setPlan(plan);
        subscription.setStatus(com.mbhoni_creative.adminentity.SubscriptionStatus.TRIAL);
        when(tenantSubscriptionRepository.findByTenant(tenant)).thenReturn(Optional.of(subscription));
        when(userRepository.countByTenantAndActiveTrue(tenant)).thenReturn(5L);
        assertThrows(RuntimeException.class, () -> userService.saveUser(dto));
        var ordered = org.mockito.Mockito.inOrder(tenantRepository, userRepository);
        ordered.verify(tenantRepository).lockById(1L); ordered.verify(userRepository).countByTenantAndActiveTrue(tenant);
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).save(any());
        org.mockito.Mockito.verifyNoInteractions(notificationService);
    }

    @Test void downgradedOverCapTenantCanReadExistingUserButCannotAddAnother() {
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(true);
        when(userRepository.findById(100L)).thenReturn(Optional.of(user));
        assertEquals(100L, userService.getUserById(100L).getId());
        var dto = new UserDto(); dto.setUsername("new"); dto.setEmail("new@example.com"); dto.setTenantId(1L);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.lockById(1L)).thenReturn(Optional.of(tenant));
        var plan = new com.mbhoni_creative.adminentity.SubscriptionPlan(); plan.setMaxUsers(5);
        var subscription = new com.mbhoni_creative.adminentity.TenantSubscription(); subscription.setPlan(plan);
        when(tenantSubscriptionRepository.findByTenant(tenant)).thenReturn(Optional.of(subscription));
        when(userRepository.countByTenantAndActiveTrue(tenant)).thenReturn(10L);
        assertTrue(assertThrows(RuntimeException.class, () -> userService.saveUser(dto)).getMessage().contains("Existing users retain access"));
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).save(any());
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).delete(any());
    }

    @Test void inactiveUsersDoNotConsumeAvailableSeats() {
        var dto = new UserDto(); dto.setUsername("new"); dto.setEmail("new@example.com"); dto.setPassword("Secret123!"); dto.setTenantId(1L);
        when(tenantSecurityService.isGlobalAdmin()).thenReturn(true);
        when(tenantRepository.findById(1L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.lockById(1L)).thenReturn(Optional.of(tenant));
        var plan = new com.mbhoni_creative.adminentity.SubscriptionPlan(); plan.setMaxUsers(5);
        var subscription = new com.mbhoni_creative.adminentity.TenantSubscription(); subscription.setPlan(plan);
        when(tenantSubscriptionRepository.findByTenant(tenant)).thenReturn(Optional.of(subscription));
        when(userRepository.countByTenantAndActiveTrue(tenant)).thenReturn(4L);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        userService.saveUser(dto);
        verify(userRepository).save(any());
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).countByTenant(any());
    }

    @Test
    void testGeneratePasswordResetToken_Success() {
        when(userRepository.findById(100L)).thenReturn(Optional.of(user));

        String token = userService.generatePasswordResetToken(100L);

        assertNotNull(token);
        assertNotNull(user.getPasswordResetToken());
        verify(userRepository).save(user);
    }
}
