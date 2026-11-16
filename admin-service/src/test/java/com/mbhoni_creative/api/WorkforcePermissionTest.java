package com.mbhoni_creative.api;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.adminservice.ModuleService;
import com.mbhoni_creative.adminservice.impl.ApiKeyServiceImpl;
import com.mbhoni_creative.api.config.WorkforcePermissionInitializer;
import com.mbhoni_creative.api.security.*;
import com.mbhoni_creative.config.*;
import java.util.*;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkforcePermissionTest {
    private TenantApiKey key(String... names) {
        var key=new TenantApiKey();
        for(String name:names) { var p=new Permission(); p.setName(name); key.getPermissions().add(p); }
        return key;
    }
    @SuppressWarnings("unchecked")
    @Test void legacyAndMixedAuthoritySetsAreExactlyTheBaselineIncludingEmptyKeys() throws Exception {
        var filter=new ApiKeyAuthenticationFilter(mock(com.mbhoni_creative.adminservice.ApiKeyService.class));
        var authorities=ApiKeyAuthenticationFilter.class.getDeclaredMethod("toAuthorities",TenantApiKey.class); authorities.setAccessible(true);
        for(String[] names:new String[][]{{},{"USER_VIEW"},{"USER_DELETE"},{"TENANT_VIEW","MODULE_VIEW"},{"UNKNOWN"},
                {"API_EMPLOYEE_READ","USER_VIEW"},{"API_ORG_READ","UNKNOWN"}}) {
            var key=key(names); Set<String> baseline=new HashSet<>(Arrays.asList(names)); baseline.add("API_KEY");
            Set<String> actual=((Set<GrantedAuthority>)authorities.invoke(filter,key)).stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
            assertEquals(baseline,actual); assertFalse(WorkforcePermissions.v1Only(key));
            assertEquals(new HashSet<>(Arrays.asList(names)),key.getPermissions().stream().map(Permission::getName).collect(Collectors.toSet()));
        }
        var only=key("API_EMPLOYEE_READ"); assertTrue(WorkforcePermissions.v1Only(only));
        assertEquals(Set.of("API_EMPLOYEE_READ"),((Set<GrantedAuthority>)authorities.invoke(filter,only)).stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()));
        assertEquals(Set.of("employees:read"),ApiScopes.from(only.getPermissions()));
    }
    @Test void existingScopeMappingsRemainIdenticalAndWorkforcePermissionsAreReadOnly() {
        Map<String,Set<String>> prior=Map.of("USER_VIEW",Set.of("users:read","access:check"),"TENANT_VIEW",Set.of("tenant:read","quotas:read"),
                "CUSTOMIZATION_VIEW",Set.of("branding:read"),"MODULE_VIEW",Set.of("modules:read"),"SUBSCRIPTION_VIEW",Set.of("entitlements:read"));
        prior.forEach((name,scopes)->assertEquals(scopes,ApiScopes.from(key(name).getPermissions())));
        for(String name:List.of("EMPLOYEE_VIEW","ORG_VIEW","MASTER_DATA_VIEW","USER_EDIT","UNKNOWN"))
            assertTrue(ApiScopes.from(key(name).getPermissions()).isEmpty());
        for(String name:WorkforcePermissions.API_ONLY) {
            var scopes=ApiScopes.from(key(name).getPermissions());
            if(name.equals("API_BILLING_WRITE")) assertEquals(Set.of("billing:write"),scopes);
            else assertTrue(scopes.stream().allMatch(s->s.endsWith(":read")));
            assertTrue(WorkforcePermissions.v1Only(key(name)));
        }
        assertEquals(Set.of("billing:write"),ApiScopes.PERMISSION_SCOPES.values().stream()
                .flatMap(Set::stream).filter(scope->scope.endsWith(":write")).collect(Collectors.toSet()));
    }
    @Test void creationServiceAllowsApiOnlyOrMixedGrantsWithoutChangingExistingKeys() throws Exception {
        var permissions=mock(PermissionRepository.class); var keys=mock(TenantApiKeyRepository.class);
        var service=new ApiKeyServiceImpl(keys,mock(TenantRepository.class),permissions,mock(TenantSecurityService.class),mock(ModuleService.class));
        var resolve=ApiKeyServiceImpl.class.getDeclaredMethod("resolvePermissions",Set.class); resolve.setAccessible(true);
        var mixed=key("API_EMPLOYEE_READ","USER_VIEW").getPermissions();
        when(permissions.findAllById(Set.of(1L,2L))).thenReturn(new ArrayList<>(mixed));
        assertEquals(mixed,resolve.invoke(service,Set.of(1L,2L)));
        service.getAllPermissions();
        verify(permissions).findByNameIn(argThat(names->names.containsAll(WorkforcePermissions.API_ONLY)&&names.contains("USER_VIEW")&&!names.contains("EMPLOYEE_EDIT")));
        verifyNoInteractions(keys);
    }
    @Test void initializerAddsOnlyNamesAndIsIdempotent() {
        var ds=new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:permission-init;MODE=MySQL;DB_CLOSE_DELAY=-1","sa","");
        var jdbc=new org.springframework.jdbc.core.JdbcTemplate(ds);
        jdbc.execute("CREATE TABLE IF NOT EXISTS permissions(id BIGINT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(100) UNIQUE,description VARCHAR(255))");
        jdbc.update("INSERT INTO permissions(name,description) VALUES('USER_VIEW','Original')");
        var initializer=new WorkforcePermissionInitializer(jdbc); initializer.run(null); initializer.run(null);
        assertEquals(4,jdbc.queryForObject("SELECT COUNT(*) FROM permissions",Integer.class));
        assertEquals("Original",jdbc.queryForObject("SELECT description FROM permissions WHERE name='USER_VIEW'",String.class));
    }
}
