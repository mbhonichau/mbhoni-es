package com.mbhoni_creative.admincontroller;

import java.util.Set;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbhoni_creative.adminservice.UserRoleAssignmentService;

@Controller
@RequestMapping("/users/roles")
public class UserRoleAssignmentController {

    private final UserRoleAssignmentService userRoleAssignmentService;

    public UserRoleAssignmentController(UserRoleAssignmentService userRoleAssignmentService) {
        this.userRoleAssignmentService = userRoleAssignmentService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public String editRoles(@RequestParam Long id, Model model) {
        model.addAttribute("userId", id);
        model.addAttribute("roles", userRoleAssignmentService.getRoleAssignmentView(id));
        return "users/roles";
    }

    @PostMapping("/update")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public String updateRoles(
            @RequestParam Long id,
            @RequestParam(required = false) Set<Long> roleIds,
            RedirectAttributes redirectAttributes) {
        userRoleAssignmentService.updateUserRoles(id, roleIds);
        redirectAttributes.addFlashAttribute("successMessage", "User roles updated successfully.");
        return "redirect:/users";
    }
}