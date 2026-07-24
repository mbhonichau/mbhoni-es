package com.mbhoni_creative.admincontroller;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mbhoni_creative.admindto.ApiAuthStatusResponse;
import com.mbhoni_creative.admindto.IntegrationUserResponse;
import com.mbhoni_creative.admindto.PagedResponse;
import com.mbhoni_creative.adminentity.Permission;
import com.mbhoni_creative.adminentity.Role;
import com.mbhoni_creative.adminentity.User;
import com.mbhoni_creative.adminrepository.UserRepository;
import com.mbhoni_creative.config.ApiKeyPrincipal;

@RestController
@RequestMapping("/api/integrations")
public class IntegrationApiController {

    private final UserRepository userRepository;

    public IntegrationApiController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('API_KEY')")
    public ApiAuthStatusResponse me(Authentication authentication) {
        ApiKeyPrincipal principal = (ApiKeyPrincipal) authentication.getPrincipal();

        Set<String> permissions = principal.getApiKey()
                .getPermissions()
                .stream()
                .map(Permission::getName)
                .collect(Collectors.toSet());

        return new ApiAuthStatusResponse(
                principal.getTenantId(),
                principal.getName(),
                permissions
        );
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('API_KEY') and hasAuthority('USER_VIEW')")
    public PagedResponse<IntegrationUserResponse> users(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {

        ApiKeyPrincipal principal = (ApiKeyPrincipal) authentication.getPrincipal();
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by("username").ascending()
        );

        Page<User> users = userRepository.findByTenantId(principal.getTenantId(), pageable);

        return new PagedResponse<>(
                users.getContent().stream().map(this::mapUser).toList(),
                users.getNumber(),
                users.getSize(),
                users.getTotalElements(),
                users.getTotalPages()
        );
    }

    @GetMapping("/users/{id}")
    @PreAuthorize("hasAuthority('API_KEY') and hasAuthority('USER_VIEW')")
    public IntegrationUserResponse user(
            Authentication authentication,
            @PathVariable Long id) {

        ApiKeyPrincipal principal = (ApiKeyPrincipal) authentication.getPrincipal();

        User user = userRepository.findByIdAndTenantId(id, principal.getTenantId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapUser(user);
    }

    private IntegrationUserResponse mapUser(User user) {
        Set<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new IntegrationUserResponse(
                user.getId(),
                user.getTenant() != null ? user.getTenant().getId() : null,
                user.getUsername(),
                user.getEmail(),
                user.isActive(),
                roles
        );
    }
}
