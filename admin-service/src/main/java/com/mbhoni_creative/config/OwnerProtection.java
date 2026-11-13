package com.mbhoni_creative.config;

import com.mbhoni_creative.adminentity.*;
import java.util.Collection;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

/** Ownership comes from the tenant record, never from a client-supplied role name. */
public final class OwnerProtection {
    private OwnerProtection() {}
    public static boolean isOwner(User user) {
        return user != null && user.getId() != null && user.getTenant() != null
                && user.getId().equals(user.getTenant().getOwnerUserId());
    }
    public static void requireSelfForOwner(User target) {
        if (!isOwner(target)) return;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserPrincipal principal)
                || !target.getId().equals(principal.getUser().getId()))
            throw new AccessDeniedException("Owner changes require the Owner or the audited ownership support flow");
    }
    public static void rejectOwnerRemoval(User target) {
        if (isOwner(target)) throw new AccessDeniedException("Transfer ownership before removing the Owner");
    }
    public static void rejectOwnerAssignment(Collection<Role> roles) {
        if (roles.stream().anyMatch(r -> "ROLE_OWNER".equals(r.getName())))
            throw new AccessDeniedException("Use the ownership transfer flow to assign Owner");
    }
}
