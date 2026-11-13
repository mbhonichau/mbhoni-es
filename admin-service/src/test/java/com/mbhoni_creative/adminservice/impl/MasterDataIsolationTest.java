package com.mbhoni_creative.adminservice.impl;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.mbhoni_creative.config.*;
import org.junit.jupiter.api.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MasterDataIsolationTest {
    private final LookupCategoryRepository categories = mock(LookupCategoryRepository.class);
    private final LookupCodeRepository codes = mock(LookupCodeRepository.class);
    private final MasterDataServiceImpl service = new MasterDataServiceImpl(categories, codes,
            mock(TenantRepository.class), new TenantAccessService(new TenantSecurityService()), mock(TenantGovernance.class));
    private LookupCategory category;
    @BeforeEach void setup() {
        Tenant own = new Tenant(); own.setId(1L);
        User user = new User(); user.setTenant(own);
        CustomUserPrincipal principal = new CustomUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        Tenant foreign = new Tenant(); foreign.setId(2L);
        category = new LookupCategory(); category.setTenant(foreign);
        org.springframework.test.util.ReflectionTestUtils.setField(category, "id", 20L);
        when(categories.findById(20L)).thenReturn(Optional.of(category));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    @Test void foreignCategoryCannotBeRead() {
        assertThrows(AccessDeniedException.class, () -> service.getLookupCodesByCategoryId(20L));
        verifyNoInteractions(codes);
    }
    @Test void foreignCategoryCannotBeWritten() {
        assertThrows(AccessDeniedException.class, () -> service.saveLookupCode(20L, new LookupCode()));
        verifyNoInteractions(codes);
    }
    @Test void ownCategoryRemainsReadable() {
        category.getTenant().setId(1L);
        assertDoesNotThrow(() -> service.getLookupCodesByCategoryId(20L));
        verify(codes).findByCategoryIdOrderByDisplayOrderAsc(20L);
    }
    @Test void foreignCodeCannotBeMovedIntoOwnCategory() {
        LookupCategory own = new LookupCategory();
        Tenant tenant = new Tenant(); tenant.setId(1L); own.setTenant(tenant);
        when(categories.findById(10L)).thenReturn(Optional.of(own));
        LookupCode existing = new LookupCode(); existing.setCategory(category);
        when(codes.findById(30L)).thenReturn(Optional.of(existing));
        LookupCode input = new LookupCode();
        org.springframework.test.util.ReflectionTestUtils.setField(input, "id", 30L);
        assertThrows(AccessDeniedException.class, () -> service.saveLookupCode(10L, input));
        verify(codes, never()).save(any());
    }
    @Test void foreignCategoryCannotBeReassignedByCreatePayload() {
        LookupCategory input = new LookupCategory();
        org.springframework.test.util.ReflectionTestUtils.setField(input, "id", 20L);
        assertThrows(AccessDeniedException.class, () -> service.createCategory(input, 1L));
        verify(categories, never()).save(any());
    }
}
