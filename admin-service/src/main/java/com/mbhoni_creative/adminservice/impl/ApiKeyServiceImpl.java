package com.mbhoni_creative.adminservice.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.admindto.ApiKeyDto;
import com.mbhoni_creative.admindto.GeneratedApiKey;
import com.mbhoni_creative.adminentity.Permission;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.TenantApiKey;
import com.mbhoni_creative.adminrepository.PermissionRepository;
import com.mbhoni_creative.adminrepository.TenantApiKeyRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminservice.ApiKeyService;
import com.mbhoni_creative.adminservice.ModuleService;
import com.mbhoni_creative.config.TenantSecurityService;
import com.mbhoni_creative.security.TenantContext;

@Service
public class ApiKeyServiceImpl implements ApiKeyService {

    private static final String KEY_PREFIX = "mbh_";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Set<String> ALLOWED_API_PERMISSIONS = Set.of(
            "USER_VIEW",
            "USER_CREATE",
            "USER_EDIT",
            "USER_DELETE",
            "TENANT_VIEW",
            "BILLING_VIEW",
            "SUBSCRIPTION_VIEW",
            "MODULE_VIEW",
            "CUSTOMIZATION_VIEW"
    );

    private final TenantApiKeyRepository tenantApiKeyRepository;
    private final TenantRepository tenantRepository;
    private final PermissionRepository permissionRepository;
    private final TenantSecurityService tenantSecurityService;
    private final ModuleService moduleService;

    public ApiKeyServiceImpl(
            TenantApiKeyRepository tenantApiKeyRepository,
            TenantRepository tenantRepository,
            PermissionRepository permissionRepository,
            TenantSecurityService tenantSecurityService,
            ModuleService moduleService) {
        this.tenantApiKeyRepository = tenantApiKeyRepository;
        this.tenantRepository = tenantRepository;
        this.permissionRepository = permissionRepository;
        this.tenantSecurityService = tenantSecurityService;
        this.moduleService = moduleService;
    }

    @Override
    public List<ApiKeyDto> getApiKeys() {
        List<TenantApiKey> apiKeys;

        if (tenantSecurityService.isGlobalAdmin()) {
            apiKeys = tenantApiKeyRepository.findAll();
        } else {
            Tenant tenant = resolveCurrentTenant();
            apiKeys = tenantApiKeyRepository.findByTenantOrderByCreatedAtDesc(tenant);
        }

        return apiKeys.stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    public ApiKeyDto prepareCreate() {
        ApiKeyDto dto = new ApiKeyDto();

        if (!tenantSecurityService.isGlobalAdmin()) {
            dto.setTenantId(resolveCurrentTenant().getId());
        }

        dto.setActive(true);
        return dto;
    }

    @Override
    public List<Permission> getAllPermissions() {
        return permissionRepository.findByNameIn(ALLOWED_API_PERMISSIONS);
    }

    @Override
    @Transactional
    public GeneratedApiKey createApiKey(ApiKeyDto dto) {
        Tenant tenant = resolveTenantForSave(dto.getTenantId());
        enforceApiAccessAllowed(tenant);
        String rawKey = generateRawKey();

        TenantApiKey apiKey = new TenantApiKey();
        apiKey.setTenant(tenant);
        apiKey.setName(normalizeName(dto.getName()));
        apiKey.setKeyPrefix(rawKey.substring(0, Math.min(rawKey.length(), 12)));
        apiKey.setKeyHash(hash(rawKey));
        apiKey.setActive(true);
        apiKey.setExpiresAt(dto.getExpiresAt());
        apiKey.setPermissions(resolvePermissions(dto.getPermissionIds()));

        TenantApiKey saved = tenantApiKeyRepository.save(apiKey);
        return new GeneratedApiKey(mapToDto(saved), rawKey);
    }

    @Override
    @Transactional
    public void revokeApiKey(Long id) {
        TenantApiKey apiKey = getApiKeyWithAccessCheck(id);
        apiKey.setActive(false);
        tenantApiKeyRepository.save(apiKey);
    }

    @Override
    @Transactional
    public TenantApiKey authenticate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            return null;
        }

        TenantApiKey apiKey = tenantApiKeyRepository.findByKeyHash(hash(rawKey.trim()))
                .orElse(null);

        if (apiKey == null || !apiKey.isActive()) {
            return null;
        }

        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }

        apiKey.setLastUsedAt(LocalDateTime.now());
        return tenantApiKeyRepository.save(apiKey);
    }

    private TenantApiKey getApiKeyWithAccessCheck(Long id) {
        TenantApiKey apiKey = tenantApiKeyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API key not found"));

        if (tenantSecurityService.isGlobalAdmin()) {
            return apiKey;
        }

        Tenant tenant = resolveCurrentTenant();

        if (apiKey.getTenant() == null || !tenant.getId().equals(apiKey.getTenant().getId())) {
            throw new RuntimeException("Access denied");
        }

        return apiKey;
    }

    private Tenant resolveTenantForSave(Long tenantId) {
        if (tenantSecurityService.isGlobalAdmin()) {
            if (tenantId == null) {
                throw new RuntimeException("Tenant is required");
            }

            return tenantRepository.findById(tenantId)
                    .orElseThrow(() -> new RuntimeException("Tenant not found"));
        }

        return resolveCurrentTenant();
    }

    private Tenant resolveCurrentTenant() {
        Long tenantId = TenantContext.getTenantId();

        if (tenantId == null) {
            throw new RuntimeException("Tenant context not set");
        }

        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));
    }

    private void enforceApiAccessAllowed(Tenant tenant) {
        if (tenant == null || !moduleService.isModuleEnabled(tenant.getId(), "API_ACCESS")) {
            throw new RuntimeException("API access is not enabled for this tenant's plan");
        }
    }

    private Set<Permission> resolvePermissions(Set<Long> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Permission> permissions = new HashSet<>(permissionRepository.findAllById(permissionIds));

        for (Permission permission : permissions) {
            if (!ALLOWED_API_PERMISSIONS.contains(permission.getName())) {
                throw new RuntimeException("Permission is not allowed for API keys: " + permission.getName());
            }
        }

        return permissions;
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("API key name is required");
        }

        return name.trim();
    }

    private String generateRawKey() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return KEY_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to hash API key", ex);
        }
    }

    private ApiKeyDto mapToDto(TenantApiKey apiKey) {
        ApiKeyDto dto = new ApiKeyDto();

        dto.setId(apiKey.getId());
        dto.setName(apiKey.getName());
        dto.setKeyPrefix(apiKey.getKeyPrefix());
        dto.setActive(apiKey.isActive());
        dto.setExpiresAt(apiKey.getExpiresAt());
        dto.setLastUsedAt(apiKey.getLastUsedAt());

        if (apiKey.getTenant() != null) {
            dto.setTenantId(apiKey.getTenant().getId());
            dto.setTenantName(apiKey.getTenant().getName());
        }

        dto.setPermissionIds(
                apiKey.getPermissions()
                        .stream()
                        .map(Permission::getId)
                        .collect(Collectors.toSet())
        );

        dto.setPermissions(
                apiKey.getPermissions()
                        .stream()
                        .map(Permission::getName)
                        .collect(Collectors.toSet())
        );

        return dto;
    }
}
