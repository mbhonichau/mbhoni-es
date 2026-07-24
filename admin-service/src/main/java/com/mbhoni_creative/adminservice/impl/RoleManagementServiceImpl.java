package com.mbhoni_creative.adminservice.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.admindto.RoleDto;
import com.mbhoni_creative.adminentity.Permission;
import com.mbhoni_creative.adminentity.Role;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminrepository.PermissionRepository;
import com.mbhoni_creative.adminrepository.RoleRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.RoleManagementService;
import com.mbhoni_creative.config.TenantSecurityService;
import com.mbhoni_creative.security.TenantContext;

@Service
public class RoleManagementServiceImpl implements RoleManagementService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final TenantRepository tenantRepository;
    private final TenantSecurityService tenantSecurityService;

    public RoleManagementServiceImpl(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            TenantRepository tenantRepository,
            TenantSecurityService tenantSecurityService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.tenantRepository = tenantRepository;
        this.tenantSecurityService = tenantSecurityService;
    }

    @Override
    public List<RoleDto> getRoles() {
        List<Role> roles;

        if (tenantSecurityService.isGlobalAdmin()) {
            roles = roleRepository.findAll();
        } else {
            Tenant tenant = resolveCurrentTenant();
            roles = roleRepository.findByTenantIdOrderByNameAsc(tenant.getId());
        }

        return roles.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public RoleDto prepareCreateRole() {
        RoleDto dto = new RoleDto();

        if (!tenantSecurityService.isGlobalAdmin()) {
            dto.setTenantId(resolveCurrentTenant().getId());
        }

        return dto;
    }

    @Override
    public RoleDto getRoleForEdit(Long id) {
        Role role = getRoleWithAccessCheck(id);
        return mapToDto(role);
    }

    @Override
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    @Override
    @Transactional
    public void createRole(RoleDto dto) {
        Tenant tenant = resolveTenantForSave(dto.getTenantId());
        String roleName = normalizeRoleName(dto.getName());

        roleRepository.findByNameAndTenantId(roleName, tenant.getId())
                .ifPresent(existing -> {
                    throw new RuntimeException("Role already exists for this tenant");
                });

        Role role = new Role();
        role.setName(roleName);
        role.setDescription(dto.getDescription());
        role.setTenant(tenant);
        role.setSystemRole(false);
        role.setPermissions(resolvePermissions(dto.getPermissionIds()));

        roleRepository.save(role);
    }

    @Override
    @Transactional
    public void updateRole(RoleDto dto) {
        Role role = getRoleWithAccessCheck(dto.getId());

        if (role.isSystemRole()) {
            throw new RuntimeException("System roles cannot be edited here");
        }

        Tenant tenant = role.getTenant();
        String roleName = normalizeRoleName(dto.getName());

        if (tenant == null) {
            throw new RuntimeException("Global roles cannot be edited here");
        }

        roleRepository.findByNameAndTenantId(roleName, tenant.getId())
                .filter(existing -> !existing.getId().equals(role.getId()))
                .ifPresent(existing -> {
                    throw new RuntimeException("Role already exists for this tenant");
                });

        role.setName(roleName);
        role.setDescription(dto.getDescription());
        role.setPermissions(resolvePermissions(dto.getPermissionIds()));

        roleRepository.save(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = getRoleWithAccessCheck(id);

        if (role.isSystemRole()) {
            throw new RuntimeException("System roles cannot be deleted");
        }

        roleRepository.delete(role);
    }

    private Role getRoleWithAccessCheck(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        if (tenantSecurityService.isGlobalAdmin()) {
            return role;
        }

        Tenant tenant = resolveCurrentTenant();

        if (role.getTenant() == null || !tenant.getId().equals(role.getTenant().getId())) {
            throw new RuntimeException("Access denied");
        }

        return role;
    }

    private Tenant resolveTenantForSave(Long tenantId) {
        if (tenantSecurityService.isGlobalAdmin()) {
            if (tenantId == null) {
                throw new RuntimeException("Tenant is required");
            }

            return tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
        }

        return resolveCurrentTenant();
    }

    private Tenant resolveCurrentTenant() {
        Long tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            throw new RuntimeException("Tenant context not set");
        }

        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
    }

    private Set<Permission> resolvePermissions(Set<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }

        return new HashSet<>(permissionRepository.findAllById(permissionIds));
    }

    private String normalizeRoleName(String name) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Role name is required");
        }

        return name.trim().toUpperCase().replace(" ", "_");
    }

    private RoleDto mapToDto(Role role) {
        RoleDto dto = new RoleDto();

        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        dto.setSystemRole(role.isSystemRole());

        if (role.getTenant() != null) {
            dto.setTenantId(role.getTenant().getId());
            dto.setTenantName(role.getTenant().getName());
        }

        dto.setPermissionIds(
                role.getPermissions()
                        .stream()
                        .map(Permission::getId)
                        .collect(Collectors.toSet())
        );

        dto.setPermissions(
                role.getPermissions()
                        .stream()
                        .map(Permission::getName)
                        .collect(Collectors.toSet())
        );

        return dto;
    }
}
