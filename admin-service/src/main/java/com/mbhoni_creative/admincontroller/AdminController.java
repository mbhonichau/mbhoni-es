package com.mbhoni_creative.admincontroller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.admindto.TenantDto;
import com.mbhoni_creative.adminservice.TenantService;

@Controller
@RequestMapping
public class AdminController {

    private final TenantService tenantService;
    private final com.mbhoni_creative.adminservice.UserService userService;

    @Autowired
    public AdminController(TenantService tenantService, com.mbhoni_creative.adminservice.UserService userService) {
        this.tenantService = tenantService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "auth/login";
    }

    @GetMapping({"/register", "/signup"})
    public String showRegisterPage(Model model) {
        model.addAttribute("tenants", tenantService.getAllTenants());
        return "auth/register";
    }

    @PostMapping({"/register", "/signup"})
    public String handleRegisterSubmit(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "tenantId", required = false) Long tenantId,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            userService.registerUser(username, email, password, tenantId);
            redirectAttributes.addFlashAttribute("registeredSuccess", true);
            return "redirect:/login?registered=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/register?error=true";
        }
    }

    @PostMapping({"/forgot-password", "/users/forgot-password"})
    public String handleForgotPassword(
            @RequestParam("username") String username,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        userService.requestPasswordReset(username);
        redirectAttributes.addFlashAttribute("resetSentSuccess", true);
        return "redirect:/login?resetSent=true";
    }

    @GetMapping("/dashboard/{id}")
    public String dashboard(
            @PathVariable("id") Long tenantId,
            Model model,
            Authentication authentication) {

        model.addAttribute("globalAdmin", false);

        try {
            TenantDto dto = tenantService.getTenantById(tenantId);
            model.addAttribute("tenant", dto);
            model.addAttribute("tenantName", dto.getName());
            model.addAttribute("tenantUsers", 0);
        } catch (Exception e) {
            model.addAttribute("tenantName", "Unknown Tenant");
            model.addAttribute("tenantUsers", 0);
        }

        model.addAttribute(
                "username",
                authentication != null
                        ? authentication.getName()
                        : "Guest"
        );

        return "dashboard";
    }

    @RestController
    @RequestMapping("/api/admin")
    public static class Api {

        private final TenantService tenantService;

        @Autowired
        public Api(TenantService tenantService) {
            this.tenantService = tenantService;
        }

        @GetMapping("/status")
        public Map<String, String> status() {
            return Map.of("status", "Admin Service Active");
        }

        @GetMapping("/tenant-metrics/{id}")
        public Map<String, Object> getTenantMetrics(
                @PathVariable("id") Long tenantId) {

            TenantDto dto = tenantService.getTenantById(tenantId);

            Map<String, Object> metrics = new LinkedHashMap<>();

            metrics.put("tenantId", dto.getId());
            metrics.put("tenantCode", dto.getId());
            metrics.put("name", dto.getName());
            metrics.put("active", dto.isActive());
            metrics.put("usage", dto.getLatestUsage());
            metrics.put("timestamp", dto.getLastUpdated());

            return metrics;
        }

        @PutMapping("/tenant/{tenantId}")
        public Map<String, String> updateTenant(
                @PathVariable Long tenantId,
                @RequestBody TenantDto dto) {

            tenantService.updateTenant(tenantId, dto);

            return Map.of("status", "Tenant updated successfully");
        }

        @DeleteMapping("/tenant/{tenantId}")
        public Map<String, String> deleteTenant(
                @PathVariable Long tenantId) {

            tenantService.deleteTenant(tenantId);

            return Map.of("status", "Tenant deleted successfully");
        }
    }
    
    @GetMapping("/access-denied")
    public String accessDenied(Model model, Authentication authentication) {

        model.addAttribute(
                "username",
                authentication != null ? authentication.getName() : "User"
        );

        return "access-denied";
    }
}