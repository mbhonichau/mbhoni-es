package com.mbhoni_creative.admincontroller.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.mbhoni_creative.admindto.PasswordChangeRequest;
import com.mbhoni_creative.admindto.UserProfileResponse;
import com.mbhoni_creative.admindto.UserProfileUpdateRequest;
import com.mbhoni_creative.adminservice.UserProfileService;

@ExtendWith(MockitoExtension.class)
class UserProfileApiControllerTest {

    @Mock
    private UserProfileService userProfileService;

    @InjectMocks
    private UserProfileApiController userProfileApiController;

    @Test
    void testGetProfile_Success() {
        UserProfileResponse expected = new UserProfileResponse();
        expected.setId(1L);
        expected.setUsername("mbuso");
        expected.setEmail("mbuso@system.local");

        when(userProfileService.getCurrentUserProfile()).thenReturn(expected);

        ResponseEntity<UserProfileResponse> response = userProfileApiController.getProfile();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("mbuso", response.getBody().getUsername());
    }

    @Test
    void testUpdateProfile_Success() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest();
        request.setFirstName("Mbhoni");
        request.setLastName("Chauke");

        UserProfileResponse expected = new UserProfileResponse();
        expected.setFirstName("Mbhoni");
        expected.setLastName("Chauke");

        when(userProfileService.updateUserProfile(request)).thenReturn(expected);

        ResponseEntity<UserProfileResponse> response = userProfileApiController.updateProfile(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Mbhoni", response.getBody().getFirstName());
    }

    @Test
    void testChangePassword_Success() {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setCurrentPassword("oldPass");
        request.setNewPassword("newPass123");
        request.setConfirmPassword("newPass123");

        ResponseEntity<Void> response = userProfileApiController.changePassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userProfileService).changeUserPassword(request);
    }
}
