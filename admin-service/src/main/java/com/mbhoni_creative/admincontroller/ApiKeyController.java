package com.mbhoni_creative.admincontroller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbhoni_creative.admindto.PageMeta;
import com.mbhoni_creative.admindto.ApiKeyDto;
import com.mbhoni_creative.admindto.GeneratedApiKey;
import com.mbhoni_creative.adminservice.ApiKeyService;
import com.mbhoni_creative.adminservice.TenantService;
import com.mbhoni_creative.config.TenantSecurityService;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/api-keys")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final TenantService tenantService;
    private final TenantSecurityService tenantSecurityService;

    public ApiKeyController(
            ApiKeyService apiKeyService,
            TenantService tenantService,
            TenantSecurityService tenantSecurityService) {
        this.apiKeyService = apiKeyService;
        this.tenantService = tenantService;
        this.tenantSecurityService = tenantSecurityService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('API_KEY_VIEW') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public String listApiKeys(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {

        List<ApiKeyDto> apiKeys = apiKeyService.getApiKeys()
                .stream()
                .filter(apiKey -> matchesApiKeySearch(apiKey, q))
                .filter(apiKey -> matchesStatus(apiKey, status))
                .toList();

        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        int fromIndex = Math.min(safePage * safeSize, apiKeys.size());
        int toIndex = Math.min(fromIndex + safeSize, apiKeys.size());

        model.addAttribute("apiKeys", apiKeys.subList(fromIndex, toIndex));
        model.addAttribute("page", new PageMeta(
                safePage,
                safeSize,
                apiKeys.size(),
                (int) Math.ceil((double) apiKeys.size() / safeSize)
        ));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        return "api-keys/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAuthority('API_KEY_CREATE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public String createApiKeyForm(Model model) {
        model.addAttribute("apiKey", apiKeyService.prepareCreate());
        model.addAttribute("permissions", apiKeyService.getAllPermissions());
        model.addAttribute("globalAdmin", tenantSecurityService.isGlobalAdmin());

        if (tenantSecurityService.isGlobalAdmin()) {
            model.addAttribute("tenants", tenantService.getAllTenants());
        }

        return "api-keys/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('API_KEY_CREATE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public String createApiKey(
            @ModelAttribute("apiKey") ApiKeyDto apiKeyDto,
            RedirectAttributes redirectAttributes) {
        GeneratedApiKey generated = apiKeyService.createApiKey(apiKeyDto);
        redirectAttributes.addFlashAttribute("successMessage", "API key created successfully.");
        redirectAttributes.addFlashAttribute("generatedApiKey", generated.getRawKey());
        return "redirect:/api-keys";
    }

    @PostMapping("/revoke")
    @PreAuthorize("hasAuthority('API_KEY_DELETE') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_TENANT_ADMIN')")
    public String revokeApiKey(
            @RequestParam Long id,
            RedirectAttributes redirectAttributes) {
        apiKeyService.revokeApiKey(id);
        redirectAttributes.addFlashAttribute("successMessage", "API key revoked successfully.");
        return "redirect:/api-keys";
    }

    private boolean matchesApiKeySearch(ApiKeyDto apiKey, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }

        String search = q.trim().toLowerCase(Locale.ROOT);
        return contains(apiKey.getName(), search)
                || contains(apiKey.getTenantName(), search)
                || contains(apiKey.getKeyPrefix(), search)
                || apiKey.getPermissions().stream().anyMatch(permission -> contains(permission, search));
    }

    private boolean matchesStatus(ApiKeyDto apiKey, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }

        if ("ACTIVE".equalsIgnoreCase(status)) {
            return apiKey.isActive();
        }

        if ("REVOKED".equalsIgnoreCase(status)) {
            return !apiKey.isActive();
        }

        return true;
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }
}
