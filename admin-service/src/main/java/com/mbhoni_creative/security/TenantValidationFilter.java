package com.mbhoni_creative.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.mbhoni_creative.config.CustomUserPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class TenantValidationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {

            Authentication authentication =
                    SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null &&
                authentication.getPrincipal() instanceof CustomUserPrincipal principal) {

                if (principal.getTenantId() != null) {
                    TenantContext.setTenantId(principal.getTenantId());
                }
            }

            filterChain.doFilter(request, response);

        } finally {

            // VERY IMPORTANT
            TenantContext.clear();
        }
    }
}