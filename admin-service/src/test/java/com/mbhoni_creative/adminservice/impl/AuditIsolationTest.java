package com.mbhoni_creative.adminservice.impl;

import com.mbhoni_creative.adminrepository.AuditLogRepository;
import com.mbhoni_creative.config.TenantSecurityService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuditIsolationTest {
    private final AuditLogRepository repository = mock(AuditLogRepository.class);
    private final TenantSecurityService security = mock(TenantSecurityService.class);
    private final AuditLogServiceImpl service = new AuditLogServiceImpl(repository, security);
    @Test void unresolvedPrincipalCannotSearchAllTenants() {
        when(security.getCurrentTenantId()).thenReturn(null);
        assertThrows(AccessDeniedException.class,
                () -> service.searchAuditLogs(null, null, null, null, Pageable.unpaged()));
        verifyNoInteractions(repository);
    }
    @Test void tenantSearchIsScoped() {
        when(security.getCurrentTenantId()).thenReturn(1L);
        service.searchAuditLogs(null, null, null, null, Pageable.unpaged());
        verify(repository).searchAuditLogs(1L, null, null, null, null, Pageable.unpaged());
    }
    @Test void globalAdminCanSearchAllTenants() {
        when(security.isGlobalAdmin()).thenReturn(true);
        service.searchAuditLogs(null, null, null, null, Pageable.unpaged());
        verify(repository).searchAuditLogs(null, null, null, null, null, Pageable.unpaged());
    }
}
