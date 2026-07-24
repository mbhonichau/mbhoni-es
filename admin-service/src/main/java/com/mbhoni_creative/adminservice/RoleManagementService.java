package com.mbhoni_creative.adminservice;

import java.util.List;

import com.mbhoni_creative.admindto.RoleDto;
import com.mbhoni_creative.adminentity.Permission;

public interface RoleManagementService {

    List<RoleDto> getRoles();

    RoleDto getRoleForEdit(Long id);

    RoleDto prepareCreateRole();

    List<Permission> getAllPermissions();

    void createRole(RoleDto dto);

    void updateRole(RoleDto dto);

    void deleteRole(Long id);
}