package com.mbhoni_creative.adminrepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mbhoni_creative.adminentity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:tenantId IS NULL OR a.tenantId = :tenantId)
        AND (:q IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(a.description) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(a.resourceId) LIKE LOWER(CONCAT('%', :q, '%')))
        AND (:action IS NULL OR a.action = :action)
        AND (:actorType IS NULL OR a.actorType = :actorType)
        AND (:resourceType IS NULL OR a.resourceType = :resourceType)
        ORDER BY a.occurredAt DESC
    """)
    Page<AuditLog> searchAuditLogs(
            @Param("tenantId") Long tenantId,
            @Param("q") String q,
            @Param("action") String action,
            @Param("actorType") String actorType,
            @Param("resourceType") String resourceType,
            Pageable pageable
    );
}