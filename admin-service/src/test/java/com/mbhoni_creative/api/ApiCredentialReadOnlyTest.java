package com.mbhoni_creative.api;

import com.mbhoni_creative.AdminServiceApplication;
import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.ModuleService;
import com.mbhoni_creative.adminservice.impl.ApiKeyServiceImpl;
import com.mbhoni_creative.api.security.ApiCredentialService;
import com.mbhoni_creative.config.TenantSecurityService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:api-credential-test;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password="}, showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = AdminServiceApplication.class)
@Import({ApiCredentialService.class, ApiKeyServiceImpl.class, TenantSecurityService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ApiCredentialReadOnlyTest {
    @Autowired ApiCredentialService credentials;
    @Autowired TenantApiKeyRepository keys;
    @Autowired TenantRepository tenants;
    @Autowired PlatformTransactionManager manager;
    @MockitoBean ModuleService modules;
    @Test void authenticatingExistingHashedKeyDoesNotPersistLastUsedAtOrOtherChanges() throws Exception {
        var tx = new TransactionTemplate(manager);
        String raw = "test-only-read-only-key";
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        var original = LocalDateTime.of(2026, 1, 1, 0, 0);
        Long id = tx.execute(status -> {
            Tenant tenant = new Tenant(); tenant.setName("Read-only auth"); tenants.save(tenant);
            TenantApiKey key = new TenantApiKey(); key.setTenant(tenant); key.setName("Read-only key");
            key.setKeyHash(hash); key.setKeyPrefix("test"); key.setLastUsedAt(original); return keys.saveAndFlush(key).getId();
        });
        assertNotNull(credentials.authenticate(raw));
        tx.executeWithoutResult(status -> assertEquals(original, keys.findById(id).orElseThrow().getLastUsedAt()));
        tx.executeWithoutResult(status -> { var key = keys.findById(id).orElseThrow(); key.setActive(false); });
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> credentials.authenticate(raw));
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class, () -> credentials.authenticate("wrong"));
    }
}
