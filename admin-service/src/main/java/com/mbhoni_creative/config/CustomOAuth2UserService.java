package com.mbhoni_creative.config;

import com.mbhoni_creative.adminentity.User;
import com.mbhoni_creative.adminrepository.UserRepository;
import com.mbhoni_creative.onboarding.TenantSignupService;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository users;
    private final TenantSignupService signup;
    public CustomOAuth2UserService(UserRepository users, TenantSignupService signup) {
        this.users = users; this.signup = signup;
    }
    @Override @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        if (!"google".equals(request.getClientRegistration().getRegistrationId()))
            throw new OAuth2AuthenticationException("Unsupported identity provider");
        return resolveVerifiedGoogle(super.loadUser(request).getAttributes());
    }
    OAuth2User resolveVerifiedGoogle(Map<String, Object> attributes) {
        Object emailValue = attributes.get("email"), subjectValue = attributes.get("sub");
        if (!Boolean.TRUE.equals(attributes.get("email_verified")) || !(emailValue instanceof String email)
                || email.isBlank() || !(subjectValue instanceof String subject) || subject.isBlank())
            throw new OAuth2AuthenticationException("Verified Google email and subject are required");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmail(normalized).orElse(null);
        if (user == null) {
            user = signup.signupGoogle(normalized, subject, (String) attributes.get("name"));
        } else if (!user.isActive() || !"GOOGLE".equals(user.getAuthProvider()) || !subject.equals(user.getProviderId())) {
            // Email equality alone is not authorization to link or take over an existing account.
            throw new OAuth2AuthenticationException("Sign in using your existing account method; explicit account linking is required");
        }
        return new TenantOAuthPrincipal(user, attributes);
    }
}
