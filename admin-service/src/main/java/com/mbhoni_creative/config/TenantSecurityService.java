package com.mbhoni_creative.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TenantSecurityService {

    public Long getCurrentTenantId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserPrincipal customUser) {

            return customUser.getTenantId();
        }

        return null;
    }

    public boolean isGlobalAdmin() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null) {
            return false;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserPrincipal customUser) {

            return customUser.isGlobalAdmin();
        }

        return false;
    }
}