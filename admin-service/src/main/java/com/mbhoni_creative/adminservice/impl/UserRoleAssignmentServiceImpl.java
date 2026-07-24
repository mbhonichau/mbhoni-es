package com.mbhoni_creative.adminservice.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mbhoni_creative.admindto.UserRoleAssignmentView;
import com.mbhoni_creative.adminentity.Role;
import com.mbhoni_creative.adminentity.User;
import com.mbhoni_creative.adminrepository.RoleRepository;
import com.mbhoni_creative.adminrepository.UserRepository;
import com.mbhoni_creative.adminservice.UserRoleAssignmentService;
import com.mbhoni_creative.config.TenantSecurityService;
import com.mbhoni_creative.security.TenantContext;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class UserRoleAssignmentServiceImpl implements UserRoleAssignmentService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantSecurityService tenantSecurityService;

    public UserRoleAssignmentServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            TenantSecurityService tenantSecurityService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tenantSecurityService = tenantSecurityService;
    }

    @Override
    public List<UserRoleAssignmentView> getRoleAssignmentView(Long userId) {
        User user = getUserWithAccessCheck(userId);

        Long tenantId = user.getTenant() != null ? user.getTenant().getId() : null;

        List<Role> availableRoles = tenantId == null
                ? roleRepository.findAll()
                : roleRepository.findByTenantIdOrderByNameAsc(tenantId);

        Set<Long> assignedRoleIds = user.getRoles()
                .stream()
                .map(Role::getId)
                .collect(Collectors.toSet());

        return availableRoles.stream()
                .map(role -> {
                    UserRoleAssignmentView view = new UserRoleAssignmentView();
                    view.setRoleId(role.getId());
                    view.setName(role.getName());
                    view.setDescription(role.getDescription());
                    view.setAssigned(assignedRoleIds.contains(role.getId()));
                    return view;
                })
                .toList();
    }

    @Override
    public void updateUserRoles(Long userId, Set<Long> roleIds) {
        User user = getUserWithAccessCheck(userId);

        Set<Long> safeRoleIds = roleIds == null ? Collections.emptySet() : roleIds;

        List<Role> selectedRoles = safeRoleIds.isEmpty()
                ? Collections.emptyList()
                : roleRepository.findByIdIn(safeRoleIds);

        Long tenantId = user.getTenant() != null ? user.getTenant().getId() : null;

        for (Role role : selectedRoles) {
            if (tenantId == null) {
                if (role.getTenant() != null) {
                    throw new RuntimeException("Global users cannot be assigned tenant roles");
                }
            } else if (role.getTenant() == null || !tenantId.equals(role.getTenant().getId())) {
                throw new RuntimeException("Selected role does not belong to this tenant");
            }
        }

        user.setRoles(new HashSet<>(selectedRoles));
        userRepository.save(user);
    }

    private User getUserWithAccessCheck(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (tenantSecurityService.isGlobalAdmin()) {
            return user;
        }

        Long tenantId = TenantContext.getTenantId();

        if (tenantId == null ||
                user.getTenant() == null ||
                !tenantId.equals(user.getTenant().getId())) {
            throw new RuntimeException("Access denied");
        }

        return user;
    }
}