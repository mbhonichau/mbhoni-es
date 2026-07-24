package com.mbhoni_creative.admincontroller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.admindto.PageMeta;
import com.mbhoni_creative.admindto.UserDto;
import com.mbhoni_creative.adminentity.Role;
import com.mbhoni_creative.adminrepository.RoleRepository;
import com.mbhoni_creative.adminservice.TenantService;
import com.mbhoni_creative.adminservice.UserService;
import com.mbhoni_creative.config.TenantSecurityService;

import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.http.ResponseEntity;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final TenantService tenantService;
    private final TenantSecurityService tenantSecurityService;
    private final RoleRepository roleRepository;
    
    public UserController(
            UserService userService,
            TenantService tenantService,
            TenantSecurityService tenantSecurityService,
            RoleRepository roleRepository) {

        this.userService = userService;
        this.tenantService = tenantService;
        this.tenantSecurityService = tenantSecurityService;
        this.roleRepository = roleRepository;
    }

    // =====================================================
    // LIST USERS
    // =====================================================

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public String listUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {

        List<UserDto> filteredUsers = userService.getAllUsers()
                .stream()
                .filter(user -> matchesUserSearch(user, q))
                .filter(user -> matchesStatus(user, status))
                .toList();

        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        int fromIndex = Math.min(safePage * safeSize, filteredUsers.size());
        int toIndex = Math.min(fromIndex + safeSize, filteredUsers.size());

        model.addAttribute("users", filteredUsers.subList(fromIndex, toIndex));
        model.addAttribute("page", new PageMeta(
                safePage,
                safeSize,
                filteredUsers.size(),
                (int) Math.ceil((double) filteredUsers.size() / safeSize)
        ));
        model.addAttribute("q", q);
        model.addAttribute("status", status);

        return "users/list";
    }

    // =====================================================
    // CREATE USER (FORM)
    // =====================================================

    @GetMapping("/create")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public String createUserForm(Model model) {

        boolean globalAdmin = tenantSecurityService.isGlobalAdmin();

        model.addAttribute("user", new UserDto());
        model.addAttribute("globalAdmin", globalAdmin);
        model.addAttribute("availableRoles", getAvailableRolesForForm());

        if (globalAdmin) {
            model.addAttribute("tenants", tenantService.getAllTenants());
        }

        return "users/create";
    }

    // =====================================================
    // CREATE USER (SUBMIT)
    // =====================================================

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public String saveUser(
            @ModelAttribute("user") UserDto userDto,
            RedirectAttributes redirectAttributes) {

        userService.saveUser(userDto);
        redirectAttributes.addFlashAttribute("successMessage", "User created successfully.");

        return "redirect:/users";
    }

	 // =====================================================
	 // EDIT USER (FORM)
	 // =====================================================
	
    @PostMapping("/edit")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public String editUserForm(
            @RequestParam Long id,
            Model model) {

        boolean globalAdmin = tenantSecurityService.isGlobalAdmin();

        model.addAttribute("user", userService.getUserById(id));
        model.addAttribute("globalAdmin", globalAdmin);
        model.addAttribute("availableRoles", getAvailableRolesForForm());

        if (globalAdmin) {
            model.addAttribute("tenants", tenantService.getAllTenants());
        }

        return "users/edit";
    }

	// =====================================================
	// UPDATE USER
	// =====================================================

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public String updateUser(
            @RequestParam Long id,
            @ModelAttribute("user") UserDto user,
            RedirectAttributes redirectAttributes) {

        user.setId(id);
        userService.updateUser(user);
        redirectAttributes.addFlashAttribute("successMessage", "User updated successfully.");

        return "redirect:/users";
    }

	// =====================================================
	// DELETE USER
	// =====================================================

    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public String deleteUser(
            @RequestParam Long id,
            RedirectAttributes redirectAttributes) {

        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("successMessage", "User deleted successfully.");

        return "redirect:/users";
    }

	// =====================================================
	// RESET PASSWORD FORM
	// =====================================================

	@PostMapping("/reset-password")
	@PreAuthorize("hasAuthority('USER_EDIT')")
	public String showResetPasswordForm(
	        @RequestParam Long id,
	        Model model) {

	    model.addAttribute("user", userService.getUserById(id));

	    return "users/reset-password";
	}

	// =====================================================
	// PROCESS PASSWORD RESET
	// =====================================================

	@PostMapping("/reset-password/update")
	@PreAuthorize("hasAuthority('USER_EDIT')")
	public String processResetPassword(
	        @RequestParam Long id,
	        @RequestParam("password") String password,
	        RedirectAttributes redirectAttributes) {

	    userService.resetPassword(id, password);
	        redirectAttributes.addFlashAttribute("successMessage", "Password reset successfully.");

	    return "redirect:/users";
	}

    private List<Role> getAvailableRolesForForm() {
        if (tenantSecurityService.isGlobalAdmin()) {
            return roleRepository.findAll();
        }

        Long tenantId = tenantSecurityService.getCurrentTenantId();

        if (tenantId == null) {
            return List.of();
        }

        return roleRepository.findByTenantIdOrderByNameAsc(tenantId);
    }

    // =====================================================
    // GENERATE TEMPORARY PASSWORD
    // =====================================================

    @GetMapping("/generate-password")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<Map<String, String>> generatePassword() {
        String password = userService.generateTemporaryPassword();
        return ResponseEntity.ok(Map.of("password", password));
    }

    // =====================================================
    // TOKEN-BASED PASSWORD RESET (PUBLIC)
    // =====================================================

    @GetMapping("/reset-password")
    public String showTokenResetForm(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        return "users/reset-password-token";
    }

    @PostMapping("/reset-password/token")
    public String processTokenReset(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            RedirectAttributes redirectAttributes) {

        boolean reset = userService.resetPasswordWithToken(token, password);

        if (!reset) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password reset link is invalid or has expired.");
            return "redirect:/login";
        }

        redirectAttributes.addFlashAttribute("successMessage", "Password reset successfully. You can now login.");
        return "redirect:/login";
    }

    private boolean matchesUserSearch(UserDto user, String q) {
        if (q == null || q.isBlank()) {
            return true;
        }

        String search = q.trim().toLowerCase(Locale.ROOT);
        return contains(user.getUsername(), search)
                || contains(user.getEmail(), search)
                || (user.getRoles() != null && user.getRoles().stream().anyMatch(role -> contains(role, search)));
    }

    private boolean matchesStatus(UserDto user, String status) {
        if (status == null || status.isBlank()) {
            return true;
        }

        if ("ACTIVE".equalsIgnoreCase(status)) {
            return user.isActive();
        }

        if ("INACTIVE".equalsIgnoreCase(status)) {
            return !user.isActive();
        }

        return true;
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }
}
