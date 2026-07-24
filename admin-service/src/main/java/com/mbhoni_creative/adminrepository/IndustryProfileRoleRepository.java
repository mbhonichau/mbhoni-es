package com.mbhoni_creative.adminrepository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.IndustryProfileRole;

public interface IndustryProfileRoleRepository extends JpaRepository<IndustryProfileRole, Long> {

    List<IndustryProfileRole> findByIndustryProfileIdOrderByRoleNameAsc(Long industryProfileId);
}
