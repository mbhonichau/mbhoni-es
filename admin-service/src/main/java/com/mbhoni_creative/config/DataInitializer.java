package com.mbhoni_creative.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.adminentity.Permission;
import com.mbhoni_creative.adminentity.Role;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.User;
import com.mbhoni_creative.adminrepository.PermissionRepository;
import com.mbhoni_creative.adminrepository.RoleRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            TenantRepository tenantRepository,
            PermissionRepository permissionRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {

        Role adminRole = getOrCreateRole("ROLE_ADMIN");
        Role userRole = getOrCreateRole("ROLE_USER");
        Role tenantAdminRole = getOrCreateRole("ROLE_TENANT_ADMIN");

        Permission tenantCreate = getOrCreatePermission("TENANT_CREATE");
        Permission tenantEdit = getOrCreatePermission("TENANT_EDIT");
        Permission tenantDelete = getOrCreatePermission("TENANT_DELETE");
        Permission tenantView = getOrCreatePermission("TENANT_VIEW");

        Permission userCreate = getOrCreatePermission("USER_CREATE");
        Permission userEdit = getOrCreatePermission("USER_EDIT");
        Permission userDelete = getOrCreatePermission("USER_DELETE");
        Permission userView = getOrCreatePermission("USER_VIEW");
        
        Permission subscriptionCreate = getOrCreatePermission("SUBSCRIPTION_CREATE");
        Permission subscriptionEdit = getOrCreatePermission("SUBSCRIPTION_EDIT");
        Permission subscriptionDelete = getOrCreatePermission("SUBSCRIPTION_DELETE");
        Permission subscriptionView = getOrCreatePermission("SUBSCRIPTION_VIEW");
        
        Permission billingView = getOrCreatePermission("BILLING_VIEW");
        Permission billingCreate = getOrCreatePermission("BILLING_CREATE");
        Permission billingEdit = getOrCreatePermission("BILLING_EDIT");
        Permission billingDelete = getOrCreatePermission("BILLING_DELETE");
        
        Permission customizationView = getOrCreatePermission("CUSTOMIZATION_VIEW");
        Permission customizationEdit = getOrCreatePermission("CUSTOMIZATION_EDIT");
        
        Permission moduleView = getOrCreatePermission("MODULE_VIEW");
        Permission moduleEdit = getOrCreatePermission("MODULE_EDIT");
        
        Permission roleView = getOrCreatePermission("ROLE_VIEW");
        Permission roleCreate = getOrCreatePermission("ROLE_CREATE");
        Permission roleEdit = getOrCreatePermission("ROLE_EDIT");
        Permission roleDelete = getOrCreatePermission("ROLE_DELETE");

        Permission apiKeyView = getOrCreatePermission("API_KEY_VIEW");
        Permission apiKeyCreate = getOrCreatePermission("API_KEY_CREATE");
        Permission apiKeyEdit = getOrCreatePermission("API_KEY_EDIT");
        Permission apiKeyDelete = getOrCreatePermission("API_KEY_DELETE");
        
        Permission auditView = getOrCreatePermission("AUDIT_VIEW");

        adminRole.setPermissions(new HashSet<>(Set.of(
                tenantCreate,
                tenantEdit,
                tenantDelete,
                tenantView,
                userCreate,
                userEdit,
                userDelete,
                userView,
                subscriptionCreate,
                subscriptionEdit,
                subscriptionDelete,
                subscriptionView,
                billingView,
                billingCreate,
                billingEdit,
                billingDelete,
                customizationView,
                customizationEdit,
                moduleView,
                moduleEdit,
                roleView,
                roleCreate,
                roleEdit,
                roleDelete,
                apiKeyView,
                apiKeyCreate,
                apiKeyEdit,
                apiKeyDelete,
                auditView
                
        )));

        tenantAdminRole.setPermissions(new HashSet<>(Set.of(
                userCreate,
                userEdit,
                userDelete,
                userView,
                customizationView,
                customizationEdit,
                moduleView,
                roleView,
                roleCreate,
                roleEdit,
                roleDelete,
                apiKeyView,
                apiKeyCreate,
                apiKeyEdit,
                apiKeyDelete,
                auditView
        )));

        userRole.setPermissions(new HashSet<>(Set.of(
                userView
        )));

        roleRepository.save(adminRole);
        roleRepository.save(tenantAdminRole);
        roleRepository.save(userRole);

        Tenant jusaquaTenant = tenantRepository.findByName("JusAqua")
                .orElseGet(() -> {
                    Tenant tenant = new Tenant();
                    tenant.setName("JusAqua");
                    tenant.setActive(true);
                    return tenantRepository.save(tenant);
                });

        User superAdmin = userRepository.findByUsername("mbuso")
                .orElseGet(User::new);

        superAdmin.setUsername("mbuso");
        superAdmin.setEmail("mbuso@system.local");

        if (superAdmin.getPassword() == null || superAdmin.getPassword().isBlank()) {
            superAdmin.setPassword(passwordEncoder.encode("password123"));
        }

        superAdmin.setTenant(null);
        superAdmin.setRoles(new HashSet<>(Set.of(adminRole, userRole)));
        superAdmin.setGlobalAdmin(true);
        superAdmin.setActive(true);

        userRepository.save(superAdmin);

        User tenantAdmin = userRepository.findByUsername("jusaqua_admin")
                .orElseGet(User::new);

        tenantAdmin.setUsername("jusaqua_admin");
        tenantAdmin.setEmail("admin@jusaqua.local");

        if (tenantAdmin.getPassword() == null || tenantAdmin.getPassword().isBlank()) {
            tenantAdmin.setPassword(passwordEncoder.encode("aqua123"));
        }

        tenantAdmin.setTenant(jusaquaTenant);
        tenantAdmin.setRoles(new HashSet<>(Set.of(tenantAdminRole)));
        tenantAdmin.setGlobalAdmin(false);
        tenantAdmin.setActive(true);

        userRepository.save(tenantAdmin);

        System.out.println(">> Default security data reconciled");
        
        
    }

    private Role getOrCreateRole(String name) {

        Role role = roleRepository.findByNameAndTenantIsNull(name)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(name);
                    newRole.setPermissions(new HashSet<>());
                    return newRole;
                });

        role.setTenant(null);
        role.setSystemRole(true);

        return roleRepository.save(role);
    }

    private Permission getOrCreatePermission(String name) {

        return permissionRepository.findByName(name)
                .orElseGet(() -> {
                    Permission permission = new Permission();
                    permission.setName(name);
                    return permissionRepository.save(permission);
                });
    }
}
