package com.mbhoni_creative.adminservice.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.admindto.UserDto;
import com.mbhoni_creative.adminentity.Role;
import com.mbhoni_creative.adminentity.SubscriptionStatus;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantSubscription;
import com.mbhoni_creative.adminentity.User;
import com.mbhoni_creative.adminrepository.RoleRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.adminrepository.UserRepository;
import com.mbhoni_creative.adminservice.AuditLogService;
import com.mbhoni_creative.adminservice.NotificationService;
import com.mbhoni_creative.adminservice.UserService;
import com.mbhoni_creative.config.TenantSecurityService;
import com.mbhoni_creative.security.TenantContext;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantSecurityService tenantSecurityService;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           TenantRepository tenantRepository,
                           TenantSubscriptionRepository tenantSubscriptionRepository,
                           PasswordEncoder passwordEncoder,
                           TenantSecurityService tenantSecurityService,
                           NotificationService notificationService,
                           AuditLogService auditLogService) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantSecurityService = tenantSecurityService;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<UserDto> getAllUsers() {

        List<User> users;

        if (tenantSecurityService.isGlobalAdmin()) {
            users = userRepository.findAll();
        } else {
            Tenant tenant = resolveTenant();
            users = userRepository.findByTenant(tenant);
        }

        return users.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserDto getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        enforceTenantAccess(user);

        return mapToDto(user);
    }

    @Override
    @Transactional
    public void saveUser(UserDto dto) {

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        Tenant tenant = resolveTenant(dto);

        enforceUserLimit(tenant);

        User user = new User();

        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setDepartment(dto.getDepartment());
        user.setJobTitle(dto.getJobTitle());
        if (dto.getTimeZone() != null) user.setTimeZone(dto.getTimeZone());

        user.setMfaEnforced(dto.isMfaEnforced());
        user.setSsoEnforced(dto.isSsoEnforced());
        user.setApiAccessAllowed(dto.isApiAccessAllowed());
        user.setAuditExtended(dto.isAuditExtended());

        boolean passwordProvided = dto.getPassword() != null && !dto.getPassword().isBlank();

        if (passwordProvided) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
            user.setPasswordChangeRequired(false);
        } else {
            user.setPassword(passwordEncoder.encode(generateTemporaryPassword()));
            user.setPasswordChangeRequired(true);
        }

        user.setTenant(tenant);
        user.setRoles(mapRoles(dto, tenant));
        user.setActive(true);

        userRepository.save(user);
        
        auditLogService.logForTenant(
                tenant != null ? tenant.getId() : null,
                "USER_CREATED",
                "USER",
                String.valueOf(user.getId()),
                "User created: " + user.getUsername()
        );

        String tenantName = tenant != null ? tenant.getName() : "Global";
        notificationService.sendNewUserAlert(user.getUsername(), user.getEmail(), tenantName);

        if (!passwordProvided) {
            String resetToken = generatePasswordResetToken(user.getId());
            String resetLink = "http://localhost:8080/users/reset-password?token=" + resetToken;
            notificationService.sendRegistrationEmail(user.getUsername(), user.getEmail(), resetLink, tenantName);
        } else {
            notificationService.sendWelcomeEmail(user.getUsername(), user.getEmail(), tenantName);
        }
    }

    @Override
    @Transactional
    public void updateUser(UserDto dto) {

        User user = userRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        enforceTenantAccess(user);

        userRepository.findByUsername(dto.getUsername())
                .filter(existing -> !existing.getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new RuntimeException("Username already exists");
                });

        userRepository.findAll()
                .stream()
                .filter(existing -> existing.getEmail().equalsIgnoreCase(dto.getEmail()))
                .filter(existing -> !existing.getId().equals(user.getId()))
                .findFirst()
                .ifPresent(existing -> {
                    throw new RuntimeException("Email already exists");
                });

        Tenant originalTenant = user.getTenant();

        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setDepartment(dto.getDepartment());
        user.setJobTitle(dto.getJobTitle());
        if (dto.getTimeZone() != null) user.setTimeZone(dto.getTimeZone());

        user.setMfaEnforced(dto.isMfaEnforced());
        user.setSsoEnforced(dto.isSsoEnforced());
        user.setApiAccessAllowed(dto.isApiAccessAllowed());
        user.setAuditExtended(dto.isAuditExtended());

        if (tenantSecurityService.isGlobalAdmin()) {
            Tenant newTenant = resolveTenant(dto);

            if (newTenant != null &&
                (originalTenant == null || !originalTenant.getId().equals(newTenant.getId()))) {
                enforceUserLimit(newTenant);
            }

            user.setTenant(newTenant);
        }

        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        user.setRoles(mapRoles(dto, user.getTenant()));

        userRepository.save(user);
        
        auditLogService.logForTenant(
                user.getTenant() != null ? user.getTenant().getId() : null,
                "USER_UPDATED",
                "USER",
                String.valueOf(user.getId()),
                "User updated: " + user.getUsername()
        );
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        enforceTenantAccess(user);

        userRepository.delete(user);
        
        auditLogService.logForTenant(
                user.getTenant() != null ? user.getTenant().getId() : null,
                "USER_DELETED",
                "USER",
                String.valueOf(user.getId()),
                "User deleted: " + user.getUsername()
        );
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        enforceTenantAccess(user);

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangeRequired(false);
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);

        userRepository.save(user);
        
        auditLogService.logForTenant(
                user.getTenant() != null ? user.getTenant().getId() : null,
                "PASSWORD_RESET",
                "USER",
                String.valueOf(user.getId()),
                "Password reset for user: " + user.getUsername()
        );

        notificationService.sendPasswordResetAlert(user.getUsername(), user.getEmail());
    }

    private UserDto mapToDto(User user) {

        UserDto dto = new UserDto();

        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setDepartment(user.getDepartment());
        dto.setJobTitle(user.getJobTitle());
        dto.setTimeZone(user.getTimeZone());
        dto.setMfaEnforced(user.isMfaEnforced());
        dto.setSsoEnforced(user.isSsoEnforced());
        dto.setApiAccessAllowed(user.isApiAccessAllowed());
        dto.setAuditExtended(user.isAuditExtended());
        dto.setActive(user.isActive());

        if (user.getTenant() != null) {
            dto.setTenantId(user.getTenant().getId());
            dto.setTenantName(user.getTenant().getName());
        }

        dto.setRoles(
                user.getRoles()
                        .stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet())
        );

        dto.setRoleIds(
                user.getRoles()
                        .stream()
                        .map(Role::getId)
                        .collect(Collectors.toSet())
        );

        return dto;
    }

    private Set<Role> mapRoles(UserDto dto, Tenant targetTenant) {

        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            Set<Role> selectedRoles = dto.getRoleIds()
                    .stream()
                    .map(roleId -> roleRepository.findById(roleId)
                            .orElseThrow(() -> new RuntimeException("Role not found")))
                    .collect(Collectors.toSet());

            validateRolesForTenant(selectedRoles, targetTenant);
            return selectedRoles;
        }

        return mapRolesByName(dto.getRoles(), targetTenant);
    }

    private Set<Role> mapRolesByName(Set<String> roleNames, Tenant targetTenant) {

        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of();
        }

        Set<Role> selectedRoles = roleNames.stream()
                .map(roleName -> findRoleByNameForTenant(roleName, targetTenant))
                .collect(Collectors.toSet());

        validateRolesForTenant(selectedRoles, targetTenant);
        return selectedRoles;
    }

    private Role findRoleByNameForTenant(String roleName, Tenant targetTenant) {

        if (targetTenant != null) {
            return roleRepository.findByNameAndTenantId(roleName, targetTenant.getId())
                    .or(() -> roleRepository.findByNameAndTenantIsNull(roleName))
                    .orElseThrow(() -> new RuntimeException("Role not found"));
        }

        return roleRepository.findByNameAndTenantIsNull(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));
    }

    private void validateRolesForTenant(Set<Role> selectedRoles, Tenant targetTenant) {

        Long targetTenantId = targetTenant != null ? targetTenant.getId() : null;

        for (Role role : selectedRoles) {
            Long roleTenantId = role.getTenant() != null ? role.getTenant().getId() : null;

            if (targetTenantId == null) {
                if (roleTenantId != null) {
                    throw new RuntimeException("Global users cannot be assigned tenant roles");
                }
                continue;
            }

            if (roleTenantId != null && roleTenantId.equals(targetTenantId)) {
                continue;
            }

            if (tenantSecurityService.isGlobalAdmin() && roleTenantId == null) {
                continue;
            }

            throw new RuntimeException("Selected role does not belong to this tenant");
        }
    }

    private Tenant resolveTenant() {

        if (tenantSecurityService.isGlobalAdmin()) {
            throw new RuntimeException("Tenant required for global context");
        }

        Long tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            throw new RuntimeException("Tenant context not set");
        }

        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
    }

    private Tenant resolveTenant(UserDto dto) {

        if (tenantSecurityService.isGlobalAdmin()) {

            if (dto.getTenantId() == null) {
                return null;
            }

            return tenantRepository.findById(dto.getTenantId())
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
        }

        return resolveTenant();
    }

    private void enforceUserLimit(Tenant tenant) {

        if (tenant == null) {
            return;
        }

        TenantSubscription subscription = tenantSubscriptionRepository.findByTenant(tenant)
                .orElse(null);

        if (subscription == null ||
            subscription.getPlan() == null ||
            subscription.getPlan().getMaxUsers() == null) {
            return;
        }
        
        if (subscription.getStatus() == SubscriptionStatus.SUSPENDED ||
        	    subscription.getStatus() == SubscriptionStatus.CANCELLED) {
        	    throw new RuntimeException(
        	            "This tenant's subscription is " + subscription.getStatus() + ". User creation is blocked."
        	    );
        	}

        int maxUsers = subscription.getPlan().getMaxUsers();
        long currentUsers = userRepository.countByTenant(tenant);

        if (currentUsers >= maxUsers) {
            throw new RuntimeException(
                    "User limit reached for this tenant's subscription plan. Limit: " + maxUsers
            );
        }
    }

    private void enforceTenantAccess(User user) {

        if (tenantSecurityService.isGlobalAdmin()) {
            return;
        }

        Long tenantId = TenantContext.getTenantId();

        if (tenantId == null ||
            user.getTenant() == null ||
            !user.getTenant().getId().equals(tenantId)) {

            throw new RuntimeException("Access denied");
        }
    }

    @Override
    public String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();
        
        for (int i = 0; i < 12; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        return password.toString();
    }

    @Override
    @Transactional
    public String generatePasswordResetToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        return token;
    }

    @Override
    @Transactional
    public boolean resetPasswordWithToken(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElse(null);

        if (user == null) {
            return false;
        }

        if (user.getPasswordResetTokenExpiry() == null || 
            user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setPasswordChangeRequired(false);
        userRepository.save(user);

        auditLogService.logForTenant(
                user.getTenant() != null ? user.getTenant().getId() : null,
                "PASSWORD_SET_WITH_TOKEN",
                "USER",
                String.valueOf(user.getId()),
                "Password set using reset token for user: " + user.getUsername()
        );
        
        return true;
    }
}
