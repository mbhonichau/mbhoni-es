package com.mbhoni_creative.admincontroller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.mbhoni_creative.admindto.PasswordChangeRequest;
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

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateProfile(@RequestBody UserProfileUpdateRequest request) {
        return ResponseEntity.ok(userProfileService.updateUserProfile(request));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePassword(@RequestBody PasswordChangeRequest request) {
        userProfileService.changeUserPassword(request);
        return ResponseEntity.ok().build();
    }
}
