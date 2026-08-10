package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mbhoni_creative.adminentity.IndustryProfileField;
import com.mbhoni_creative.adminentity.TargetEntity;

@Repository
public interface IndustryProfileFieldRepository extends JpaRepository<IndustryProfileField, Long> {

    List<IndustryProfileField> findByIndustryProfileIdOrderByDisplayOrderAsc(Long industryProfileId);

    List<IndustryProfileField> findByIndustryProfileIdAndTargetEntityOrderByDisplayOrderAsc(Long industryProfileId, TargetEntity targetEntity);

    Optional<IndustryProfileField> findByIndustryProfileIdAndTargetEntityAndFieldKey(Long industryProfileId, TargetEntity targetEntity, String fieldKey);
}
