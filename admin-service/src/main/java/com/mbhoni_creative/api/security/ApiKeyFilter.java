package com.mbhoni_creative.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mbhoni_creative.api.error.ApiProblems;
import com.mbhoni_creative.security.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.ArrayList;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Created only inside the v1 chain; deliberately not a servlet-filter bean. */
public class ApiKeyFilter extends OncePerRequestFilter {
    private final ApiCredentialService credentials;
    private final ObjectMapper mapper;
    public ApiKeyFilter(ApiCredentialService credentials, ObjectMapper mapper) { this.credentials = credentials; this.mapper = mapper; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        TenantContext.clear(); SecurityContextHolder.clearContext();
        try {
            String key = request.getHeader("X-API-Key");
            if (key != null && !key.isBlank()) {
                try {
                    var principal = credentials.authenticate(key);
                    var scopes = ApiScopes.from(principal.getApiKey().getPermissions());
                    var authorities = new ArrayList<SimpleGrantedAuthority>();
                    scopes.forEach(scope -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));
                    if (!scopes.isEmpty()) authorities.add(new SimpleGrantedAuthority("API_V1"));
                    var context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
                    SecurityContextHolder.setContext(context); TenantContext.setTenantId(principal.getTenantId());
                } catch (org.springframework.security.core.AuthenticationException ex) {
                    ApiProblems.write(mapper, response, 401, "UNAUTHENTICATED", "A valid API key is required"); return;
                } catch (RuntimeException ex) {
                    ApiProblems.write(mapper, response, 500, "INTERNAL_ERROR", "Authentication could not be completed"); return;
                }
            }
            chain.doFilter(request, response);
        } finally { TenantContext.clear(); SecurityContextHolder.clearContext(); }
    }
}
