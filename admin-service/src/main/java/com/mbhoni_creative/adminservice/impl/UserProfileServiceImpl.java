package com.mbhoni_creative.adminservice.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.admindto.PasswordChangeRequest;
import com.mbhoni_creative.admindto.UserProfileResponse;
import com.mbhoni_creative.admindto.UserProfileUpdateRequest;
import com.mbhoni_creative.adminentity.Permission;
import com.mbhoni_creative.adminentity.Role;
import com.mbhoni_creative.adminentity.User;
import com.mbhoni_creative.adminrepository.UserRepository;
import com.mbhoni_creative.adminservice.UserProfileService;
import com.mbhoni_creative.config.CustomUserPrincipal;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile() {
        User user = resolveAuthenticatedUser();
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateUserProfile(UserProfileUpdateRequest request) {
        User user = resolveAuthenticatedUser();

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String newEmail = request.getEmail().trim().toLowerCase();
            if (!newEmail.equals(user.getEmail())) {
                if (userRepository.existsByEmail(newEmail)) {
                    throw new RuntimeException("Email already in use by another account");
                }
                user.setEmail(newEmail);
            }
        }

        userRepository.save(user);
        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public void changeUserPassword(PasswordChangeRequest request) {
        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            throw new RuntimeException("New password cannot be empty");
        }
        if (request.getConfirmPassword() != null && !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        User user = resolveAuthenticatedUser();

        if (request.getCurrentPassword() != null && !request.getCurrentPassword().isBlank()) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("Incorrect current password");
            }
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangeRequired(false);
        userRepository.save(user);
    }

    private User resolveAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("Not authenticated");
        }

        String username = auth.getName();
        if (auth.getPrincipal() instanceof CustomUserPrincipal principal) {
            username = principal.getUsername();
        }

        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.findByEmail(auth.getName())
                        .orElseThrow(() -> new RuntimeException("User profile not found for: " + auth.getName())));
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setAuthProvider(user.getAuthProvider() != null ? user.getAuthProvider() : "LOCAL");
        response.setGlobalAdmin(user.isGlobalAdmin());
        response.setActive(user.isActive());

        if (user.getTenant() != null) {
            response.setTenantId(user.getTenant().getId());
            response.setTenantName(user.getTenant().getName());
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        response.setRoles(roleNames);

        Set<String> permissions = new HashSet<>();
        for (Role role : user.getRoles()) {
            if (role.getPermissions() != null) {
                permissions.addAll(role.getPermissions().stream()
                        .map(Permission::getName)
                        .collect(Collectors.toSet()));
            }
        }
        response.setPermissions(permissions);

        return response;
    }
}
