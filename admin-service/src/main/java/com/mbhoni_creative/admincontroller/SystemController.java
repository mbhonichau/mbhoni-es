package com.mbhoni_creative.admincontroller;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.UserRepository;

@Controller
@RequestMapping("/system")
public class SystemController {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final Environment environment;

    public SystemController(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            Environment environment) {

        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.environment = environment;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN') or hasAuthority('AUDIT_VIEW')")
    public String systemPage(Model model) {

        Runtime runtime = Runtime.getRuntime();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

        long maxMemoryMb = runtime.maxMemory() / (1024 * 1024);
        long totalMemoryMb = runtime.totalMemory() / (1024 * 1024);
        long freeMemoryMb = runtime.freeMemory() / (1024 * 1024);
        long usedMemoryMb = totalMemoryMb - freeMemoryMb;

        Map<String, Object> systemMetrics = new LinkedHashMap<>();
        systemMetrics.put("javaVersion", System.getProperty("java.version"));
        systemMetrics.put("javaVendor", System.getProperty("java.vendor"));
        systemMetrics.put("osName", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        systemMetrics.put("uptimeMinutes", runtimeBean.getUptime() / (1000 * 60));
        systemMetrics.put("availableProcessors", runtime.availableProcessors());
        systemMetrics.put("usedMemoryMb", usedMemoryMb);
        systemMetrics.put("maxMemoryMb", maxMemoryMb);
        systemMetrics.put("activeProfiles", String.join(", ", environment.getActiveProfiles().length > 0 ? environment.getActiveProfiles() : new String[]{"default"}));

        model.addAttribute("systemMetrics", systemMetrics);
        model.addAttribute("totalTenants", tenantRepository.count());
        model.addAttribute("activeTenants", tenantRepository.countByActive(true));
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("swaggerUrl", "/swagger-ui/index.html");

        return "system";
    }
}