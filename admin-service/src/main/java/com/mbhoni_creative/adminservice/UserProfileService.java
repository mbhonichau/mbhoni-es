package com.mbhoni_creative.adminservice;

import com.mbhoni_creative.admindto.PasswordChangeRequest;
import com.mbhoni_creative.admindto.UserProfileResponse;
import com.mbhoni_creative.admindto.UserProfileUpdateRequest;

public interface UserProfileService {

    UserProfileResponse getCurrentUserProfile();

    UserProfileResponse updateUserProfile(UserProfileUpdateRequest request);

    void changeUserPassword(PasswordChangeRequest request);
}
