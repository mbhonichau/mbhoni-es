package com.mbhoni_creative.adminservice;

import java.util.List;
import java.util.Set;

import com.mbhoni_creative.admindto.UserRoleAssignmentView;

public interface UserRoleAssignmentService {

    List<UserRoleAssignmentView> getRoleAssignmentView(Long userId);

    void updateUserRoles(Long userId, Set<Long> roleIds);
}
