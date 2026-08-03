package com.mbhoni_creative.config;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.adminentity.BillingCycle;
import com.mbhoni_creative.adminentity.Permission;
import com.mbhoni_creative.adminentity.PlatformModule;
import com.mbhoni_creative.adminentity.Role;
import com.mbhoni_creative.adminentity.SubscriptionPlan;
import com.mbhoni_creative.adminentity.SubscriptionPlanModule;
import com.mbhoni_creative.adminentity.SubscriptionStatus;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantModule;
import com.mbhoni_creative.adminentity.TenantSubscription;
import com.mbhoni_creative.adminentity.User;
import com.mbhoni_creative.adminrepository.PermissionRepository;
import com.mbhoni_creative.adminrepository.PlatformModuleRepository;
import com.mbhoni_creative.adminrepository.RoleRepository;
import com.mbhoni_creative.adminrepository.SubscriptionPlanModuleRepository;
import com.mbhoni_creative.adminrepository.SubscriptionPlanRepository;
import com.mbhoni_creative.adminrepository.TenantModuleRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.TenantSubscriptionRepository;
import com.mbhoni_creative.adminrepository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final PermissionRepository permissionRepository;
    private final PlatformModuleRepository platformModuleRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final SubscriptionPlanModuleRepository subscriptionPlanModuleRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final TenantModuleRepository tenantModuleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            RoleRepository roleRepository,
            TenantRepository tenantRepository,
            PermissionRepository permissionRepository,
            PlatformModuleRepository platformModuleRepository,
            SubscriptionPlanRepository subscriptionPlanRepository,
            SubscriptionPlanModuleRepository subscriptionPlanModuleRepository,
            TenantSubscriptionRepository tenantSubscriptionRepository,
            TenantModuleRepository tenantModuleRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
        this.permissionRepository = permissionRepository;
        this.platformModuleRepository = platformModuleRepository;
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.subscriptionPlanModuleRepository = subscriptionPlanModuleRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.tenantModuleRepository = tenantModuleRepository;
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

        Permission employeeView = getOrCreatePermission("EMPLOYEE_VIEW");
        Permission employeeEdit = getOrCreatePermission("EMPLOYEE_EDIT");

        Permission orgView = getOrCreatePermission("ORG_VIEW");
        Permission orgEdit = getOrCreatePermission("ORG_EDIT");

        Permission contractView = getOrCreatePermission("CONTRACT_VIEW");
        Permission contractEdit = getOrCreatePermission("CONTRACT_EDIT");

        Permission masterDataView = getOrCreatePermission("MASTER_DATA_VIEW");
        Permission masterDataEdit = getOrCreatePermission("MASTER_DATA_EDIT");

        Permission serviceView = getOrCreatePermission("SERVICE_VIEW");
        Permission serviceEdit = getOrCreatePermission("SERVICE_EDIT");

        adminRole.setPermissions(new HashSet<>(Set.of(
                tenantCreate, tenantEdit, tenantDelete, tenantView,
                userCreate, userEdit, userDelete, userView,
                subscriptionCreate, subscriptionEdit, subscriptionDelete, subscriptionView,
                billingView, billingCreate, billingEdit, billingDelete,
                customizationView, customizationEdit,
                moduleView, moduleEdit,
                roleView, roleCreate, roleEdit, roleDelete,
                apiKeyView, apiKeyCreate, apiKeyEdit, apiKeyDelete,
                auditView,
                employeeView, employeeEdit,
                orgView, orgEdit,
                contractView, contractEdit,
                masterDataView, masterDataEdit,
                serviceView, serviceEdit
        )));

        tenantAdminRole.setPermissions(new HashSet<>(Set.of(
                userCreate, userEdit, userDelete, userView,
                customizationView, customizationEdit,
                moduleView,
                roleView, roleCreate, roleEdit, roleDelete,
                apiKeyView, apiKeyCreate, apiKeyEdit, apiKeyDelete,
                auditView,
                employeeView, employeeEdit,
                orgView, orgEdit,
                contractView, contractEdit,
                masterDataView, masterDataEdit,
                serviceView, serviceEdit
        )));

        userRole.setPermissions(new HashSet<>(Set.of(
                userView,
                employeeView,
                orgView,
                contractView,
                masterDataView,
                serviceView
        )));

        roleRepository.save(adminRole);
        roleRepository.save(tenantAdminRole);
        roleRepository.save(userRole);

        // Seed Platform Modules
        PlatformModule employeeModule = getOrCreateModule("EMPLOYEE", "Employee Directory & HCM", "HCM & Payroll", "Personnel records, job titles, department assignments, and employment status.");
        PlatformModule orgModule = getOrCreateModule("ORG", "Organization Units", "Business Modules", "Departments, divisions, cost centers, and hierarchy.");
        PlatformModule contractModule = getOrCreateModule("CONTRACT", "Contract Management", "Business Modules", "Vendor, customer, and employment contract tracking.");
        PlatformModule masterDataModule = getOrCreateModule("MASTER_DATA", "Master Data & Lookups", "System Setup", "System reference codes and lookup categories.");
        PlatformModule serviceModule = getOrCreateModule("SERVICE", "Service Catalog", "Business Modules", "Service catalog and SLA management.");

        List<PlatformModule> allModules = List.of(employeeModule, orgModule, contractModule, masterDataModule, serviceModule);

        // Seed Default Subscription Plan
        SubscriptionPlan defaultPlan = subscriptionPlanRepository.findByCode("ENTERPRISE")
                .orElseGet(() -> {
                    SubscriptionPlan plan = new SubscriptionPlan();
                    plan.setCode("ENTERPRISE");
                    plan.setName("Enterprise Tier");
                    plan.setDescription("Full featured enterprise platform suite");
                    plan.setMonthlyPrice(new BigDecimal("299.00"));
                    plan.setAnnualPrice(new BigDecimal("2990.00"));
                    plan.setMaxUsers(500);
                    plan.setMaxStorageMb(50000);
                    plan.setApiAccessEnabled(true);
                    plan.setBrandingEnabled(true);
                    plan.setCustomDomainEnabled(true);
                    plan.setActive(true);
                    return subscriptionPlanRepository.save(plan);
                });

        // Allow all modules for the default plan
        for (PlatformModule module : allModules) {
            subscriptionPlanModuleRepository.findByPlanAndModule(defaultPlan, module)
                    .orElseGet(() -> {
                        SubscriptionPlanModule pm = new SubscriptionPlanModule();
                        pm.setPlan(defaultPlan);
                        pm.setModule(module);
                        pm.setAllowed(true);
                        return subscriptionPlanModuleRepository.save(pm);
                    });
        }

        // Seed Default Tenant
        Tenant jusaquaTenant = tenantRepository.findByName("JusAqua")
                .orElseGet(() -> {
                    Tenant tenant = new Tenant();
                    tenant.setName("JusAqua");
                    tenant.setActive(true);
                    return tenantRepository.save(tenant);
                });

        // Assign Subscription to Tenant
        tenantSubscriptionRepository.findByTenant(jusaquaTenant)
                .orElseGet(() -> {
                    TenantSubscription sub = new TenantSubscription();
                    sub.setTenant(jusaquaTenant);
                    sub.setPlan(defaultPlan);
                    sub.setStatus(SubscriptionStatus.ACTIVE);
                    sub.setBillingCycle(BillingCycle.MONTHLY);
                    sub.setAutoRenew(true);
                    return tenantSubscriptionRepository.save(sub);
                });

        // Enable all modules for Default Tenant
        for (PlatformModule module : allModules) {
            tenantModuleRepository.findByTenantAndModule(jusaquaTenant, module)
                    .orElseGet(() -> {
                        TenantModule tm = new TenantModule();
                        tm.setTenant(jusaquaTenant);
                        tm.setModule(module);
                        tm.setEnabled(true);
                        return tenantModuleRepository.save(tm);
                    });
        }

        // Seed Users
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

        System.out.println(">> Default security data and platform modules initialized");
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

    private PlatformModule getOrCreateModule(String code, String name, String category, String description) {
        return platformModuleRepository.findByCode(code)
                .orElseGet(() -> {
                    PlatformModule module = new PlatformModule();
                    module.setCode(code);
                    module.setName(name);
                    module.setCategory(category);
                    module.setDescription(description);
                    module.setActive(true);
                    return platformModuleRepository.save(module);
                });
    }
}
