package com.mbhoni_creative.contentmanagementservice;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "admin-service")
public interface AdminServiceClient {
    @GetMapping("/api/admin/tenant-metrics/{tenantId}")
    String getTenantMetrics(@PathVariable("tenantId") String tenantId);
}