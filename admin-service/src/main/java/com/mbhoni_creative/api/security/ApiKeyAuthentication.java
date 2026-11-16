package com.mbhoni_creative.api.security;

import com.mbhoni_creative.config.ApiKeyPrincipal;
import java.util.Collection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/** Stable audit identity only for the stateless v1 chain. */
public final class ApiKeyAuthentication extends UsernamePasswordAuthenticationToken {
    public ApiKeyAuthentication(ApiKeyPrincipal principal, Collection<? extends GrantedAuthority> authorities) {
        super(principal, null, authorities);
    }
    @Override public String getName() {
        return "API_KEY:" + ((ApiKeyPrincipal)getPrincipal()).getApiKey().getId();
    }
}
