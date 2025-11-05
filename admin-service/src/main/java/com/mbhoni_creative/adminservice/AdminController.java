package com.mbhoni_creative.adminservice;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final TenantMetricRepository repo;

    public AdminController(TenantMetricRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of("status", "Admin Service Active");
    }

    @GetMapping("/tenant-metrics/{tenantId}")
    public String getMetrics(@PathVariable String tenantId) {
        return repo.findById(tenantId)
            .map(metric -> "Usage: " + metric.getUsage() + " at " + metric.getTimestamp())  // ← EXPLICIT
            .orElse("No metrics for " + tenantId);
    }
}