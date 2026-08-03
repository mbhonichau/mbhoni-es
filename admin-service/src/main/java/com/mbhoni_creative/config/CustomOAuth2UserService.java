package com.mbhoni_creative.config;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mbhoni_creative.adminentity.Permission;
import com.mbhoni_creative.adminentity.Role;
import com.mbhoni_creative.adminentity.Tenant;
import com.mbhoni_creative.adminentity.User;
import com.mbhoni_creative.adminrepository.RoleRepository;
import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.UserRepository;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;

    public CustomOAuth2UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            TenantRepository tenantRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tenantRepository = tenantRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String givenName = (String) attributes.get("given_name");
        String familyName = (String) attributes.get("family_name");
        String picture = (String) attributes.get("picture");
        String providerId = oAuth2User.getName(); // Google sub ID

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("OAuth2 email missing");
        }

        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseGet(() -> createOAuth2User(email, name, givenName, familyName, picture, providerId));

        user.setAuthProvider("GOOGLE");
        user.setProviderId(providerId);
        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            user.setAvatarUrl(picture);
        }
        userRepository.save(user);

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                authorities.add(new SimpleGrantedAuthority(role.getName()));
                if (role.getPermissions() != null) {
                    for (Permission permission : role.getPermissions()) {
                        authorities.add(new SimpleGrantedAuthority(permission.getName()));
                    }
                }
            }
        }
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            authorities.add(new SimpleGrantedAuthority("USER_VIEW"));
        }

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new DefaultOAuth2User(authorities, attributes, userNameAttributeName);
    }

    private User createOAuth2User(String email, String name, String givenName, String familyName, String picture, String providerId) {
        User newUser = new User();
        newUser.setEmail(email.toLowerCase());
        newUser.setUsername(email.toLowerCase());
        newUser.setPassword("{noop}OAUTH2_EXTERNAL_USER");
        newUser.setFirstName(givenName != null ? givenName : name);
        newUser.setLastName(familyName);
        newUser.setAvatarUrl(picture);
        newUser.setAuthProvider("GOOGLE");
        newUser.setProviderId(providerId);
        newUser.setActive(true);
        newUser.setGlobalAdmin(false);

        Role userRole = roleRepository.findByNameAndTenantIsNull("ROLE_USER")
                .orElseGet(() -> roleRepository.findAll().isEmpty() ? null : roleRepository.findAll().get(0));

        if (userRole != null) {
            newUser.setRoles(Set.of(userRole));
        }

        Tenant defaultTenant = tenantRepository.findAll().isEmpty() ? null : tenantRepository.findAll().get(0);
        newUser.setTenant(defaultTenant);

        return userRepository.save(newUser);
    }
}
