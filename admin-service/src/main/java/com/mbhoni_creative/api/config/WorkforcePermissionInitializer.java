package com.mbhoni_creative.api.config;

import com.mbhoni_creative.api.security.WorkforcePermissions;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adds permission names only; never assigns them to an existing key or role. */
@Component
public class WorkforcePermissionInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    public WorkforcePermissionInitializer(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Override @Transactional
    public void run(ApplicationArguments args) {
        for (String name : WorkforcePermissions.API_ONLY) {
            // MySQL atomic upsert also supports concurrent application starts.
            jdbc.update("INSERT INTO permissions(name, description) VALUES (?, ?) ON DUPLICATE KEY UPDATE name = name",
                    name, "Read-only v1 workforce API permission");
        }
    }
}
