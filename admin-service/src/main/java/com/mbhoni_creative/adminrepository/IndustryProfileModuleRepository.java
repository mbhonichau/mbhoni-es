package com.mbhoni_creative.adminrepository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mbhoni_creative.adminentity.IndustryProfile;
import com.mbhoni_creative.adminentity.IndustryProfileModule;

public interface IndustryProfileModuleRepository extends JpaRepository<IndustryProfileModule, Long> {

    List<IndustryProfileModule> findByIndustryProfileAndEnabledByDefaultTrue(IndustryProfile industryProfile);
}