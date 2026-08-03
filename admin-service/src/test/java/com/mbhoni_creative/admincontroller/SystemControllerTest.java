package com.mbhoni_creative.admincontroller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import com.mbhoni_creative.adminrepository.TenantRepository;
import com.mbhoni_creative.adminrepository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SystemControllerTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Environment environment;

    @InjectMocks
    private SystemController systemController;

    @Test
    void testSystemPage_ReturnsSystemViewAndAttributes() {
        when(tenantRepository.count()).thenReturn(5L);
        when(tenantRepository.countByActive(true)).thenReturn(4L);
        when(userRepository.count()).thenReturn(10L);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        Model model = new ConcurrentModel();
        String view = systemController.systemPage(model);

        assertEquals("system", view);
        assertEquals(5L, model.getAttribute("totalTenants"));
        assertEquals(4L, model.getAttribute("activeTenants"));
        assertEquals(10L, model.getAttribute("totalUsers"));
        assertEquals("/swagger-ui/index.html", model.getAttribute("swaggerUrl"));
        assertNotNull(model.getAttribute("systemMetrics"));
    }
}
