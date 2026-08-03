package com.mbhoni_creative.admincontroller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.mbhoni_creative.admindto.PasswordChangeRequest;
import com.mbhoni_creative.admindto.UserProfileResponse;
import com.mbhoni_creative.admindto.UserProfileUpdateRequest;
import com.mbhoni_creative.adminservice.UserProfileService;

@Controller
@RequestMapping("/profile")
public class UserProfileViewController {

    private final UserProfileService userProfileService;

    public UserProfileViewController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public String viewProfile(Model model) {
        UserProfileResponse profile = userProfileService.getCurrentUserProfile();
        model.addAttribute("profile", profile);
        model.addAttribute("updateRequest", new UserProfileUpdateRequest());
        model.addAttribute("passwordRequest", new PasswordChangeRequest());
        return "profile";
    }

    @PostMapping("/update")
    @PreAuthorize("isAuthenticated()")
    public String updateProfile(
            @ModelAttribute UserProfileUpdateRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            userProfileService.updateUserProfile(request);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public String changePassword(
            @ModelAttribute PasswordChangeRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            userProfileService.changeUserPassword(request);
            redirectAttributes.addFlashAttribute("successMessage", "Password changed successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile";
    }
}
