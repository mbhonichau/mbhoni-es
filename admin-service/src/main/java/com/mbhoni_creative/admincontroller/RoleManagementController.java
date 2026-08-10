package com.mbhoni_creative.admincontroller;

import java.util.List;
import java.util.Locale;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbhoni_creative.admindto.PageMeta;
import com.mbhoni_creative.admindto.RoleDto;
import com.mbhoni_creative.adminservice.RoleManagementService;
import com.mbhoni_creative.adminservice.TenantService;
import com.mbhoni_creative.config.TenantSecurityService;

@Controller
@RequestMapping("/roles")
public class RoleManagementController {

    private final RoleManagementService roleManagementService;
    private final TenantService tenantService;
    private final TenantSecurityService tenantSecurityService;

    public RoleManagementController(
            RoleManagementService roleManagementService,
            TenantService tenantService,
            TenantSecurityService tenantSecurityService) {
        this.roleManagementService = roleManagementService;
        this.tenantService = tenantService;
        this.tenantSecurityService = tenantSecurityService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public String listRoles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {

        List<RoleDto> roles = roleManagementService.getRoles()
                .stream()
                .filter(role -> matchesRoleSearch(role, q))
                .filter(role -> matchesRoleType(role, type))
                .toList();

        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        int fromIndex = Math.min(safePage * safeSize, roles.size());
        int toIndex = Math.min(fromIndex + safeSize, roles.size());

        model.addAttribute("roles", roles.subList(fromIndex, toIndex));
        model.addAttribute("page", new PageMeta(
                safePage,
                safeSize,
                roles.size(),
                (int) Math.ceil((double) roles.size() / safeSize)
        ));
        model.addAttribute("q", q);
        model.addAttribute("type", type);
        return "roles/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public String createRoleForm(Model model) {
        model.addAttribute("role", roleManagementService.prepareCreateRole());
        model.addAttribute("permissions", roleManagementService.getAllPermissions());
        model.addAttribute("globalAdmin", tenantSecurityService.isGlobalAdmin());

        if (tenantSecurityService.isGlobalAdmin()) {
            model.addAttribute("tenants", tenantService.getAllTenants());
        }

        return "roles/create";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public String createRole(
            @ModelAttribute("role") RoleDto roleDto,
            RedirectAttributes redirectAttributes) {
        roleManagementService.createRole(roleDto);
        redirectAttributes.addFlashAttribute("successMessage", "Role created successfully.");
        return "redirect:/roles";
    }

    @PostMapping("/edit")
    @PreAuthorize("hasAuthority('ROLE_EDIT')")
    public String editRoleForm(@RequestParam Long id, Model model) {
        model.addAttribute("role", roleManagementService.getRoleForEdit(id));
        model.addAttribute("permissions", roleManagementService.getAllPermissions());
        model.addAttribute("globalAdmin", tenantSecurityService.isGlobalAdmin());
        return "roles/edit";
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('ROLE_EDIT')")
    public String updateRole(
            @RequestParam(required = false) Long id,
            @ModelAttribute("role") RoleDto roleDto,
            RedirectAttributes redirectAttributes) {
        if (id != null) {
            roleDto.setId(id);
        }
        roleManagementService.updateRole(roleDto);
        redirectAttributes.addFlashAttribute("successMessage", "Role updated successfully.");
        return "redirect:/roles";
    }

    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('ROLE_DELETE')")
    public String deleteRole(
            @RequestParam Long id,
            RedirectAttributes redirectAttributes) {
        roleManagementService.deleteRole(id);
        redirectAttributes.addFlashAttribute("successMessage", "Role deleted successfully.");
        return "redirect:/roles";
    }

    private boolean matchesRoleSearch(RoleDto role, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }

        String search = q.trim().toLowerCase(Locale.ROOT);
        return contains(role.getName(), search)
                || contains(role.getDescription(), search)
                || contains(role.getTenantName(), search)
                || role.getPermissions().stream().anyMatch(permission -> contains(permission, search));
    }

    private boolean matchesRoleType(RoleDto role, String type) {
        if (type == null || type.isBlank()) {
            return true;
        }

        if ("SYSTEM".equalsIgnoreCase(type)) {
            return role.isSystemRole();
        }

        if ("CUSTOM".equalsIgnoreCase(type)) {
            return !role.isSystemRole();
        }

        return true;
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }
}
