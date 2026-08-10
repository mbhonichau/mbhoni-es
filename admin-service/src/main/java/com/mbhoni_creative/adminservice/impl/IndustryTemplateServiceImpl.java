package com.mbhoni_creative.adminservice.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mbhoni_creative.adminentity.IndustryProfileModule;
import com.mbhoni_creative.adminentity.IndustryProfileRole;
import com.mbhoni_creative.adminentity.Permission;
import com.mbhoni_creative.adminentity.Role;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantModule;
import com.mbhoni_creative.adminrepository.IndustryProfileModuleRepository;
import com.mbhoni_creative.adminrepository.IndustryProfileRoleRepository;
import com.mbhoni_creative.adminrepository.PermissionRepository;
import com.mbhoni_creative.adminrepository.RoleRepository;
import com.mbhoni_creative.adminrepository.TenantModuleRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.IndustryTemplateService;

import jakarta.transaction.Transactional;

import com.mbhoni_creative.adminservice.TenantOnboardingFieldService;

@Service
@Transactional
public class IndustryTemplateServiceImpl implements IndustryTemplateService {

    private final TenantRepository tenantRepository;
    private final IndustryProfileModuleRepository industryProfileModuleRepository;
    private final IndustryProfileRoleRepository industryProfileRoleRepository;
    private final TenantModuleRepository tenantModuleRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantOnboardingFieldService onboardingFieldService;

    public IndustryTemplateServiceImpl(
            TenantRepository tenantRepository,
            IndustryProfileModuleRepository industryProfileModuleRepository,
            IndustryProfileRoleRepository industryProfileRoleRepository,
            TenantModuleRepository tenantModuleRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            TenantOnboardingFieldService onboardingFieldService) {
        this.tenantRepository = tenantRepository;
        this.industryProfileModuleRepository = industryProfileModuleRepository;
        this.industryProfileRoleRepository = industryProfileRoleRepository;
        this.tenantModuleRepository = tenantModuleRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.onboardingFieldService = onboardingFieldService;
    }

    @Override
    public void applyTemplateToTenant(Long tenantId) {
        applyModuleTemplateToTenant(tenantId);
        applyRoleTemplateToTenant(tenantId);

        Tenant tenant = getTenantWithIndustryProfile(tenantId);
        onboardingFieldService.applyIndustryProfileFields(tenantId, tenant.getIndustryProfile().getId());
    }

    @Override
    public void applyModuleTemplateToTenant(Long tenantId) {
        Tenant tenant = getTenantWithIndustryProfile(tenantId);

        List<IndustryProfileModule> templateModules =
                industryProfileModuleRepository.findByIndustryProfileAndEnabledByDefaultTrue(
                        tenant.getIndustryProfile()
                );

        for (IndustryProfileModule templateModule : templateModules) {
            TenantModule tenantModule =
                    tenantModuleRepository
                            .findByTenantAndModule(tenant, templateModule.getModule())
                            .orElseGet(TenantModule::new);

            tenantModule.setTenant(tenant);
            tenantModule.setModule(templateModule.getModule());
            tenantModule.setEnabled(templateModule.isEnabledByDefault());

            tenantModuleRepository.save(tenantModule);
        }
    }

    @Override
    public void applyRoleTemplateToTenant(Long tenantId) {
        Tenant tenant = getTenantWithIndustryProfile(tenantId);

        List<IndustryProfileRole> templateRoles =
                industryProfileRoleRepository.findByIndustryProfileIdOrderByRoleNameAsc(
                        tenant.getIndustryProfile().getId()
                );

        for (IndustryProfileRole templateRole : templateRoles) {
            Role role = roleRepository.findByNameAndTenantId(templateRole.getRoleCode(), tenant.getId())
                    .orElseGet(Role::new);

            role.setName(templateRole.getRoleCode());
            role.setDescription(templateRole.getDescription());
            role.setSystemRole(templateRole.isSystemRole());
            role.setTenant(tenant);

            Set<String> permissionNames = templateRole.getPermissions()
                    .stream()
                    .map(permission -> permission.getPermissionCode())
                    .collect(Collectors.toSet());

            Set<Permission> permissions = new HashSet<>(permissionRepository.findByNameIn(permissionNames));
            role.setPermissions(permissions);

            roleRepository.save(role);
        }
    }

    private Tenant getTenantWithIndustryProfile(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        if (tenant.getIndustryProfile() == null) {
            throw new RuntimeException("Tenant does not have an industry profile assigned");
        }

        return tenant;
    }
}