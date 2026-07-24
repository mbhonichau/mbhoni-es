package com.mbhoni_creative.adminservice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mbhoni_creative.adminentity.AuditLog;

public interface AuditLogService {

    void log(String action, String resourceType, String resourceId, String description);

    void logForTenant(Long tenantId, String action, String resourceType, String resourceId, String description);

    Page<AuditLog> searchAuditLogs(String q, String action, String actorType, String resourceType, Pageable pageable);
}