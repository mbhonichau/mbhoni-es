package com.mbhoni_creative.api.config;

import com.mbhoni_creative.api.security.WorkforcePermissions;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BillingPermissionInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public BillingPermissionInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        for (String name : WorkforcePermissions.API_ONLY) {
            if (!name.startsWith("API_BILLING_")) continue;
            jdbc.update("INSERT INTO permissions(name, description) VALUES (?, ?) ON DUPLICATE KEY UPDATE name = name",
                    name, name.endsWith("WRITE") ? "Create invoice and quotation drafts through v1 when billing writes are enabled" : "Read tenant-issued billing documents through v1");
        }
    }
}
