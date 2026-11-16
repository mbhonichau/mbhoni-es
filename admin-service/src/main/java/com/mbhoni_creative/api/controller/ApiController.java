package com.mbhoni_creative.api.controller;

import com.mbhoni_creative.api.dto.*;
import com.mbhoni_creative.api.dto.ApiDtos.*;
import com.mbhoni_creative.api.service.ApiReadService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@SecurityRequirement(name = "ApiKey")
public class ApiController {
    private final ApiReadService service;
    public ApiController(ApiReadService service) { this.service = service; }
    @GetMapping("/users/me")
    public ApiEnvelope<Identity> me() { return ApiEnvelope.single(service.me()); }
    @GetMapping("/users/{id}") @PreAuthorize("hasAuthority('SCOPE_users:read')")
    public ApiEnvelope<UserProfile> user(@PathVariable Long id) {
        if (id < 1) throw new IllegalArgumentException("Positive user ID required");
        return ApiEnvelope.single(service.user(id));
    }
    @PostMapping("/access/check") @PreAuthorize("hasAuthority('SCOPE_access:check')")
    public ApiEnvelope<AccessResult> check(@Valid @RequestBody AccessCheck request) { return ApiEnvelope.single(service.check(request)); }
    @GetMapping("/tenant") @PreAuthorize("hasAuthority('SCOPE_tenant:read')")
    public ApiEnvelope<TenantProfile> tenant() { return ApiEnvelope.single(service.profile()); }
    @GetMapping("/tenant/branding") @PreAuthorize("hasAuthority('SCOPE_branding:read')")
    public ApiEnvelope<Branding> branding() { return ApiEnvelope.single(service.branding()); }
    @GetMapping("/tenant/quotas") @PreAuthorize("hasAuthority('SCOPE_quotas:read')")
    public ApiEnvelope<Quotas> quotas() { return ApiEnvelope.single(service.quotas()); }
    @GetMapping("/tenant/modules") @PreAuthorize("hasAuthority('SCOPE_modules:read')")
    public ApiEnvelope<List<ApiDtos.Module>> modules(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String updatedSince) {
        return service.modules(page, size, com.mbhoni_creative.api.mapper.ApiTimestamp.parse(updatedSince));
    }
    @GetMapping("/tenant/entitlements/{module}") @PreAuthorize("hasAuthority('SCOPE_entitlements:read')")
    public ApiEnvelope<Entitlement> entitlement(@PathVariable String module) { return ApiEnvelope.single(service.entitlement(module)); }
}
