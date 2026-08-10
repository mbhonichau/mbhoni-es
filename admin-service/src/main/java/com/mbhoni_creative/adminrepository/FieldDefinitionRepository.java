package com.mbhoni_creative.adminrepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.mbhoni_creative.adminentity.FieldDefinition;
import com.mbhoni_creative.adminentity.FieldSection;

@Repository
public interface FieldDefinitionRepository extends JpaRepository<FieldDefinition, Long> {

    List<FieldDefinition> findByTenantIdAndSectionAndIsActiveTrueOrderByDisplayOrderAsc(Long tenantId, FieldSection section);

    List<FieldDefinition> findByTenantIdIsNullAndSectionAndIsActiveTrueOrderByDisplayOrderAsc(FieldSection section);

    List<FieldDefinition> findByTenantIdOrderBySectionAscDisplayOrderAsc(Long tenantId);

    List<FieldDefinition> findByTenantIdIsNullOrderBySectionAscDisplayOrderAsc();

    Optional<FieldDefinition> findByTenantIdAndSectionAndFieldKey(Long tenantId, FieldSection section, String fieldKey);

    Optional<FieldDefinition> findByTenantIdIsNullAndSectionAndFieldKey(FieldSection section, String fieldKey);

    /**
     * Fallback resolution helper: Returns tenant-specific active field definitions if defined,
     * otherwise falls back to global (tenantId IS NULL) active field definitions for the section.
     */
    default List<FieldDefinition> findActiveFieldsWithFallback(Long tenantId, FieldSection section) {
        if (tenantId != null) {
            List<FieldDefinition> tenantFields = findByTenantIdAndSectionAndIsActiveTrueOrderByDisplayOrderAsc(tenantId, section);
            if (!tenantFields.isEmpty()) {
                return tenantFields;
            }
        }
        return findByTenantIdIsNullAndSectionAndIsActiveTrueOrderByDisplayOrderAsc(section);
    }
}
