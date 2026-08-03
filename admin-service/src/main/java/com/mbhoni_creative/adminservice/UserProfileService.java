package com.mbhoni_creative.adminservice;

import com.mbhoni_creative.admindto.PasswordChangeRequest;
import com.mbhoni_creative.admindto.ProcessTogglesUpdateRequest;
import com.mbhoni_creative.admindto.UserProfileResponse;
import com.mbhoni_creative.admindto.UserProfileUpdateRequest;

public interface UserProfileService {

    UserProfileResponse getCurrentUserProfile();

    UserProfileResponse getUserProfileById(Long userId);

    UserProfileResponse updateUserProfile(UserProfileUpdateRequest request);

    UserProfileResponse updateUserProcessToggles(Long userId, ProcessTogglesUpdateRequest request);

    void changeUserPassword(PasswordChangeRequest request);
}
