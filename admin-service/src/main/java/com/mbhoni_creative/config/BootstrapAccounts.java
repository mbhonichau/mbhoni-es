package com.mbhoni_creative.config;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class BootstrapAccounts {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final Environment environment;
    public BootstrapAccounts(UserRepository users, PasswordEncoder passwords, Environment environment) {
        this.users = users; this.passwords = passwords; this.environment = environment;
    }
    public void initialize(Role platformRole, Role tenantRole, Tenant demoTenant) {
        if (users.countPlatformAdministrators() == 0) {
            String username = environment.getRequiredProperty("app.bootstrap.admin-username");
            String email = environment.getRequiredProperty("app.bootstrap.admin-email");
            String password = environment.getRequiredProperty("app.bootstrap.admin-password");
            requireStrongPassword(password);
            if (username.isBlank() || email.isBlank() || users.existsByUsername(username) || users.existsByEmail(email))
                throw new IllegalStateException("Bootstrap identity must be new; existing accounts are never promoted by startup");
            User admin = new User(); admin.setUsername(username); admin.setEmail(email); admin.setPassword(passwords.encode(password));
            admin.setGlobalAdmin(true); admin.setRoles(Set.of(platformRole)); admin.setPasswordChangeRequired(true);
            users.save(admin);
        }
        String demoPassword = environment.getProperty("app.bootstrap.demo-admin-password");
        if (demoPassword != null && users.findByUsername("jusaqua_admin").isEmpty()) {
            requireStrongPassword(demoPassword);
            User demo = new User(); demo.setUsername("jusaqua_admin"); demo.setEmail("admin@jusaqua.local");
            demo.setPassword(passwords.encode(demoPassword)); demo.setTenant(demoTenant); demo.setRoles(Set.of(tenantRole));
            demo.setPasswordChangeRequired(true); users.save(demo);
        }
    }
    private static void requireStrongPassword(String password) {
        if (password == null || password.length() < 16 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Bootstrap password must be at least 16 characters and at most 72 UTF-8 bytes");
    }
}
