package com.mbhoni_creative.adminservice;

import java.util.List;
import com.mbhoni_creative.admindto.UserDto;

public interface UserService {

    List<UserDto> getAllUsers();

    UserDto getUserById(Long id);

    void saveUser(UserDto dto);

    void updateUser(UserDto dto);

    void deleteUser(Long id);

    void resetPassword(Long id, String newPassword);

    String generateTemporaryPassword();

    String generatePasswordResetToken(Long userId);

    boolean requestPasswordReset(String usernameOrEmail);

    boolean resetPasswordWithToken(String token, String newPassword);

    void registerUser(String username, String email, String password, Long tenantId);
}