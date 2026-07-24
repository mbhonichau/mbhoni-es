package com.mbhoni_creative.adminservice.impl;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.mbhoni_creative.adminentity.AuditLog;
import com.mbhoni_creative.adminrepository.AuditLogRepository;
import com.mbhoni_creative.adminservice.AuditLogService;
import com.mbhoni_creative.config.ApiKeyPrincipal;
import com.mbhoni_creative.config.CustomUserPrincipal;
import com.mbhoni_creative.config.TenantSecurityService;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final TenantSecurityService tenantSecurityService;

    public AuditLogServiceImpl(
            AuditLogRepository auditLogRepository,
            TenantSecurityService tenantSecurityService) {
        this.auditLogRepository = auditLogRepository;
        this.tenantSecurityService = tenantSecurityService;
    }

    @Override
    public void log(String action, String resourceType, String resourceId, String description) {
        logForTenant(resolveTenantId(), action, resourceType, resourceId, description);
    }

    @Override
    public void logForTenant(Long tenantId, String action, String resourceType, String resourceId, String description) {
        AuditLog auditLog = new AuditLog();

        auditLog.setTenantId(tenantId);
        auditLog.setUsername(resolveUsername());
        auditLog.setActorType(resolveActorType());
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setDescription(description);
        auditLog.setSuccess(true);
        auditLog.setOccurredAt(LocalDateTime.now());

        HttpServletRequest request = currentRequest();

        if (request != null) {
            auditLog.setIpAddress(resolveIpAddress(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
        }

        auditLogRepository.save(auditLog);
    }

    @Override
    public Page<AuditLog> searchAuditLogs(
            String q,
            String action,
            String actorType,
            String resourceType,
            Pageable pageable) {

        Long tenantId = tenantSecurityService.isGlobalAdmin()
                ? null
                : tenantSecurityService.getCurrentTenantId();

        return auditLogRepository.searchAuditLogs(
                tenantId,
                clean(q),
                clean(action),
                clean(actorType),
                clean(resourceType),
                pageable
        );
    }

    private String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Long resolveTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return tenantSecurityService.getCurrentTenantId();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserPrincipal customUser) {
            return customUser.getTenantId();
        }

        if (principal instanceof ApiKeyPrincipal apiKeyPrincipal) {
            return apiKeyPrincipal.getTenantId();
        }

        return tenantSecurityService.getCurrentTenantId();
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return "SYSTEM";
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof ApiKeyPrincipal apiKeyPrincipal) {
            return "API_KEY:" + apiKeyPrincipal.getName();
        }

        return authentication.getName();
    }

    private String resolveActorType() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return "SYSTEM";
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof ApiKeyPrincipal) {
            return "API_KEY";
        }

        if (principal instanceof CustomUserPrincipal customUser && customUser.isGlobalAdmin()) {
            return "GLOBAL_ADMIN";
        }

        if (principal instanceof CustomUserPrincipal) {
            return "TENANT_USER";
        }

        return "SYSTEM";
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }

        return null;
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}