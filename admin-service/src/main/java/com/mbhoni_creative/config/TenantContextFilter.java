package com.mbhoni_creative.config;

import com.mbhoni_creative.adminentity.User;
import com.mbhoni_creative.adminrepository.UserRepository;
import com.mbhoni_creative.security.TenantContext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public TenantContextFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.getPrincipal() instanceof CustomUserPrincipal principal) {
                if (principal.getUsername() != null && !principal.getUsername().isBlank()) {
                    Optional<User> freshUserOpt = userRepository.findByUsername(principal.getUsername());
                    if (freshUserOpt.isPresent()) {
                        principal = new CustomUserPrincipal(freshUserOpt.get());
                        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                                principal,
                                auth.getCredentials(),
                                principal.getAuthorities()
                        );
                        newAuth.setDetails(auth.getDetails());

                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(newAuth);
                        SecurityContextHolder.setContext(context);
                        securityContextRepository.saveContext(context, request, response);
                        auth = newAuth;
                    }
                }

                if (!principal.isGlobalAdmin()) {
                    TenantContext.setTenantId(principal.getTenantId());
                }
            } else if (auth != null && auth.getPrincipal() instanceof ApiKeyPrincipal principal) {
                TenantContext.setTenantId(principal.getTenantId());
            }

            if (TenantContext.getTenantId() == null && isGlobalAdmin(auth)) {
                String tenantHeader = request.getHeader("X-Tenant-ID");
                if (tenantHeader != null && !tenantHeader.isBlank()) {
                    try {
                        TenantContext.setTenantId(Long.parseLong(tenantHeader.trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            filterChain.doFilter(request, response);

        } finally {
            TenantContext.clear();
        }
    }

    private boolean isGlobalAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getPrincipal() instanceof CustomUserPrincipal principal
                && principal.isGlobalAdmin();
    }
}
