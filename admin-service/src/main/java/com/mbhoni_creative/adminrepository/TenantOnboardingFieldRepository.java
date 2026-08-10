package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mbhoni_creative.adminentity.TargetEntity;
import com.mbhoni_creative.adminentity.TenantOnboardingField;

@Repository
public interface TenantOnboardingFieldRepository extends JpaRepository<TenantOnboardingField, Long> {

    List<TenantOnboardingField> findByTenantIdOrderByDisplayOrderAsc(Long tenantId);

    List<TenantOnboardingField> findByTenantIdAndTargetEntityOrderByDisplayOrderAsc(Long tenantId, TargetEntity targetEntity);

    Optional<TenantOnboardingField> findByTenantIdAndTargetEntityAndFieldKey(Long tenantId, TargetEntity targetEntity, String fieldKey);
}
