package com.mbhoni_creative.config;

import com.mbhoni_creative.adminentity.*;
import com.mbhoni_creative.adminrepository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrganizationAccessServiceTest {
    final OrganizationMembershipRepository memberships = mock(OrganizationMembershipRepository.class);
    final TenantRepository tenants = mock(TenantRepository.class);
    final TenantSecurityService security = new TenantSecurityService();
    final OrganizationAccessService access = new OrganizationAccessService(memberships, new TenantGovernance(tenants, security), security);
    Tenant tenant; User user; OrganizationUnit root, isolated, child;
    @BeforeEach void setup() {
        tenant = new Tenant(); tenant.setId(1L); tenant.setOwnerUserId(99L);
        user = new User(); user.setId(10L); user.setTenant(tenant);
        Role admin = new Role(); admin.setName("ROLE_TENANT_ADMIN"); admin.setSystemRole(true); admin.setTenant(tenant); user.getRoles().add(admin);
        var principal = new CustomUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        when(tenants.findById(1L)).thenReturn(Optional.of(tenant));
        root = unit(1L, null); isolated = unit(2L, root); isolated.setIsolated(true); child = unit(3L, isolated);
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    OrganizationUnit unit(Long id, OrganizationUnit parent) {
        OrganizationUnit unit = new OrganizationUnit(); org.springframework.test.util.ReflectionTestUtils.setField(unit, "id", id); unit.setTenant(tenant); unit.setParentUnit(parent); return unit;
    }
    void member(OrganizationUnit unit, boolean admin) {
        OrganizationMembership membership = new OrganizationMembership(); membership.setUser(user); membership.setUnit(unit); membership.setAdministrator(admin);
        when(memberships.findByUserIdAndUnitTenantId(10L, 1L)).thenReturn(List.of(membership));
    }
    @Test void descriptiveHierarchyIsVisibleButTopAdminDoesNotBypassIsolation() {
        assertTrue(access.canAccess(root)); assertFalse(access.canAccess(isolated)); assertFalse(access.canAccess(child));
    }
    @Test void ownerBypassesRestrictedBoundaryWithoutMembership() {
        tenant.setOwnerUserId(10L); isolated.setRestrictParentVisibility(true);
        assertTrue(access.canAccess(child)); verifyNoInteractions(memberships);
    }
    @Test void descendantMembershipPermitsItsIsolatedBoundary() {
        isolated.setRestrictParentVisibility(true); member(child, false);
        assertTrue(access.canAccess(isolated)); assertTrue(access.canAccess(child));
    }
    @Test void parentAdminInheritanceStopsAtRestrictedBoundary() {
        member(root, true); assertTrue(access.canAccess(child));
        isolated.setRestrictParentVisibility(true); assertFalse(access.canAccess(child));
    }
    @Test void ordinaryParentMemberAndSiblingMemberDoNotInherit() {
        member(root, false); assertFalse(access.canAccess(child));
        member(unit(4L, root), true); assertFalse(access.canAccess(child));
    }
    @Test void nestedRestrictionRequiresMembershipInsideNestedNode() {
        member(isolated, true); child.setIsolated(true); child.setRestrictParentVisibility(true);
        assertTrue(access.canAccess(isolated)); assertFalse(access.canAccess(child));
        member(child, false); assertTrue(access.canAccess(child));
    }
    @Test void cyclesAndForeignTenantNodesFailClosed() {
        root.setParentUnit(child); assertFalse(access.canAccess(root));
        Tenant other = new Tenant(); other.setId(2L); root.setTenant(other); assertFalse(access.canAccess(root));
    }
    @Test void jsonDoesNotExpandCredentialsManagersOrOrgChildren() throws Exception {
        user.setPassword("never-serialize"); Employee employee = new Employee(); employee.setId(5L); employee.setUser(user);
        Employee manager = new Employee(); manager.setId(6L); manager.setFirstName("hidden-manager"); employee.setManager(manager);
        root.getChildUnits().add(isolated); employee.setOrganizationUnit(root);
        var json = new ObjectMapper().valueToTree(employee);
        assertFalse(json.has("user")); assertFalse(json.has("manager"));
        assertEquals(10L, json.get("userId").asLong()); assertEquals(6L, json.get("managerId").asLong());
        assertFalse(json.get("organizationUnit").has("childUnits")); assertFalse(json.get("organizationUnit").has("parentUnit"));
        assertFalse(json.toString().contains("never-serialize"));
    }
}
