package com.mbhoni_creative.config;

import com.mbhoni_creative.adminentity.User;
import java.util.Map;
import org.springframework.security.oauth2.core.user.OAuth2User;

public class TenantOAuthPrincipal extends CustomUserPrincipal implements OAuth2User {
    private final Map<String, Object> attributes;
    public TenantOAuthPrincipal(User user, Map<String, Object> attributes) {
        super(user); this.attributes = Map.copyOf(attributes);
    }
    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public String getName() { return getUsername(); }
}
