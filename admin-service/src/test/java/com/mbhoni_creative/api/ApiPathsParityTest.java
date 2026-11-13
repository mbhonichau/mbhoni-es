package com.mbhoni_creative.api;

import com.mbhoni_creative.api.security.ApiPaths;
import com.mbhoni_creative.config.ApiKeyAuthenticationFilter;
import com.mbhoni_creative.config.TenantContextFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.firewall.RequestRejectedException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiPathsParityTest {
    @Test void sharedRuleMatchesContextPathsTrailingSlashesAndCaseSensitively() throws Exception {
        for (String context : new String[]{"", "/admin"}) {
            for (String path : new String[]{"/api/v1", "/api/v1/", "/api/v1/employees", "/api/docs", "/api/docs/", "/api/openapi.json",
                    "/api/employees", "/api/employees/", "/API/v1/employees", "/api/V1/employees"}) {
                var request = new MockHttpServletRequest("GET", context + path);
                request.setContextPath(context); request.setServletPath(path);
                var safe = new StrictHttpFirewall().getFirewalledRequest(request);
                boolean expected = path.startsWith("/api/v1") || path.startsWith("/api/docs") || path.equals("/api/openapi.json");
                assertEquals(expected, ApiPaths.MATCHER.matches(safe), path);
                for (Class<?> type : new Class<?>[]{ApiKeyAuthenticationFilter.class, TenantContextFilter.class}) {
                    var method = type.getDeclaredMethod("shouldNotFilter", HttpServletRequest.class);
                    method.setAccessible(true);
                    assertEquals(expected, method.invoke(mock(type, CALLS_REAL_METHODS), safe), type + " " + path);
                }
                request.setServletPath("");
                assertEquals(expected, ApiPaths.matches(request), "URI fallback: " + path);
            }
        }
    }
    @Test void ambiguousPathsAreRejectedBeforeAnySecurityChainIsSelected() {
        for (String path : new String[]{"/api/v1;jsessionid=abc/employees", "/api/v1/employees;jsessionid=abc",
                "/api//v1/employees", "/api/v1//employees", "/api/v1/../employees", "/api/employees/../v1/employees"}) {
            var request = new MockHttpServletRequest("GET", path); request.setServletPath(path);
            assertThrows(RequestRejectedException.class, () -> new StrictHttpFirewall().getFirewalledRequest(request), path);
        }
    }
}
