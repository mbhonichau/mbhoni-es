package com.mbhoni_creative.api;

import com.mbhoni_creative.AdminServiceApplication;
import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.PermissionRepository;
import com.mbhoni_creative.api.security.ApiKeyAuthentication;
import com.mbhoni_creative.config.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties={"spring.flyway.enabled=false","spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:api-audit;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password="},showSql=false)
@AutoConfigureTestDatabase(replace=AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes=AdminServiceApplication.class)
@Import(JpaAuditConfig.class)
class ApiAuditAttributionTest {
    @Autowired PermissionRepository permissions;
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void v1WritesUseStableKeyIdAndUiWritesStillUseTheActualUsername() {
        var key=new TenantApiKey(); ReflectionTestUtils.setField(key,"id",42L);key.setName("Mutable integration label");
        SecurityContextHolder.getContext().setAuthentication(new ApiKeyAuthentication(new ApiKeyPrincipal(key),List.of()));
        var api=new Permission();api.setName("AUDIT_API_TEST");permissions.saveAndFlush(api);
        assertEquals("API_KEY:42",api.getCreatedBy());assertEquals("API_KEY:42",api.getUpdatedBy());
        var user=new User();user.setUsername("actual-ui-user");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(new CustomUserPrincipal(user),null,List.of()));
        var ui=new Permission();ui.setName("AUDIT_UI_TEST");permissions.saveAndFlush(ui);
        assertEquals("actual-ui-user",ui.getCreatedBy());assertEquals("actual-ui-user",ui.getUpdatedBy());
        assertEquals("API_KEY:42",api.getCreatedBy());
    }
}
