package com.mbhoni_creative.admincontroller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.admindto.PasswordChangeRequest;
import com.mbhoni_creative.admindto.ProcessTogglesUpdateRequest;
import com.mbhoni_creative.admindto.UserProfileResponse;
import com.mbhoni_creative.admindto.UserProfileUpdateRequest;
import com.mbhoni_creative.adminservice.UserProfileService;

@RestController
@RequestMapping("/api/user")
public class UserProfileApiController {

    private final UserProfileService userProfileService;

    public UserProfileApiController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getProfile() {
        return ResponseEntity.ok(userProfileService.getCurrentUserProfile());
    }

    @GetMapping("/profile/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW') or hasAuthority('GLOBAL_ADMIN')")
    public ResponseEntity<UserProfileResponse> getProfileById(@PathVariable Long id) {
        return ResponseEntity.ok(userProfileService.getUserProfileById(id));
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateProfile(@RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(userProfileService.updateUserProfile(request));
    }

    @PutMapping("/profile/{id}/process-toggles")
    @PreAuthorize("hasAuthority('USER_EDIT') or hasAuthority('GLOBAL_ADMIN')")
    public ResponseEntity<UserProfileResponse> updateProcessToggles(
            @PathVariable Long id,
            @RequestBody ProcessTogglesUpdateRequest request) {
        return ResponseEntity.ok(userProfileService.updateUserProcessToggles(id, request));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@RequestBody PasswordChangeRequest request) {
        userProfileService.changeUserPassword(request);
        return ResponseEntity.ok().build();
    }
}
