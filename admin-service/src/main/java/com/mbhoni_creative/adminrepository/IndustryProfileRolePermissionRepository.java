package com.mbhoni_creative.adminrepository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.IndustryProfileRolePermission;

public interface IndustryProfileRolePermissionRepository extends JpaRepository<IndustryProfileRolePermission, Long> {

    List<IndustryProfileRolePermission> findByIndustryProfileRoleId(Long industryProfileRoleId);
}