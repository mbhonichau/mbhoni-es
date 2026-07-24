package com.mbhoni_creative.admincontroller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mbhoni_creative.adminservice.AuditLogService;

@Controller
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasAuthority('AUDIT_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public String auditLogs(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) String resourceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {

        model.addAttribute(
                "auditLogs",
                auditLogService.searchAuditLogs(
                        q,
                        action,
                        actorType,
                        resourceType,
                        PageRequest.of(Math.max(page, 0), Math.min(size, 100), Sort.by("occurredAt").descending())
                )
        );

        model.addAttribute("q", q);
        model.addAttribute("action", action);
        model.addAttribute("actorType", actorType);
        model.addAttribute("resourceType", resourceType);

        return "audit-logs/list";
    }
}