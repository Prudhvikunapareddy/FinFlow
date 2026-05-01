package com.finflow.auth_service.SERVICE_LAYER;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.finflow.auth_service.DTOs.LoginRequest;
import com.finflow.auth_service.DTOs.ChangePasswordRequest;
import com.finflow.auth_service.DTOs.ProfileUpdateRequest;
import com.finflow.auth_service.DTOs.RoleUpdateRequest;
import com.finflow.auth_service.DTOs.SignupRequest;
import com.finflow.auth_service.DTOs.UserResponse;
import com.finflow.auth_service.Entity.User;
import com.finflow.auth_service.REPOSITORY.UserRepository;
import com.finflow.auth_service.UTIL.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void signupShouldCreateUserWithEncodedPasswordAndDefaultRole() {
        SignupRequest request = validSignupRequest();

        when(userRepository.findByEmail("user@finflow.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtUtil.generateToken("user@finflow.com", "USER")).thenReturn("jwt-token");

        String result = authService.signup(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("user@finflow.com", savedUser.getEmail());
        assertEquals("Asha", savedUser.getFirstName());
        assertEquals("Sharma", savedUser.getLastName());
        assertEquals(LocalDate.now().minusYears(24), savedUser.getDateOfBirth());
        assertEquals("9876543210", savedUser.getPhoneNumber());
        assertEquals("FRIEND10", savedUser.getReferralCode());
        assertEquals("encoded-secret", savedUser.getPassword());
        assertEquals("USER", savedUser.getRole());
        assertEquals("jwt-token", result);
    }

    @Test
    void signupShouldRejectExistingEmail() {
        SignupRequest request = validSignupRequest();

        when(userRepository.findByEmail("user@finflow.com")).thenReturn(Optional.of(new User()));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.signup(request));

        assertEquals("Email already registered!", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginShouldReturnJwtForValidCredentials() {
        LoginRequest request = new LoginRequest();
        request.setEmail(" user@finflow.com ");
        request.setPassword("secret");

        User user = new User();
        user.setEmail("user@finflow.com");
        user.setPassword("encoded-secret");
        user.setRole(" admin ");

        when(userRepository.findByEmail("user@finflow.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "encoded-secret")).thenReturn(true);
        when(jwtUtil.generateToken("user@finflow.com", "ADMIN")).thenReturn("jwt-token");

        assertEquals("jwt-token", authService.login(request));
    }

    @Test
    void loginShouldRejectUnknownUser() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@finflow.com");

        when(userRepository.findByEmail("user@finflow.com")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(request));

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void loginShouldRejectInvalidPassword() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@finflow.com");
        request.setPassword("wrong");

        User user = new User();
        user.setEmail("user@finflow.com");
        user.setPassword("encoded-secret");
        user.setRole("USER");

        when(userRepository.findByEmail("user@finflow.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-secret")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> authService.login(request));

        assertEquals("Invalid password", exception.getMessage());
    }

    @Test
    void getUsersShouldReturnOrderedUserResponses() {
        User user = sampleUser();

        when(userRepository.findAllByOrderByIdAsc()).thenReturn(List.of(user));

        List<UserResponse> result = authService.getUsers();

        assertEquals(1, result.size());
        assertEquals("ADMIN", result.getFirst().getRole());
        assertEquals("user@finflow.com", result.getFirst().getEmail());
    }

    @Test
    void updateUserRoleShouldNormalizeAndPersistRole() {
        User user = sampleUser();
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setRole(" user ");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse result = authService.updateUserRole(2L, request);

        assertEquals("USER", user.getRole());
        assertEquals("USER", result.getRole());
    }

    @Test
    void updateUserRoleShouldRejectInvalidRole() {
        User user = sampleUser();
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setRole("manager");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.updateUserRole(2L, request));

        assertEquals("Invalid role: manager", exception.getMessage());
    }

    @Test
    void getProfileShouldReturnUserProfile() {
        User user = sampleUser();

        when(userRepository.findByEmail("user@finflow.com")).thenReturn(Optional.of(user));

        UserResponse result = authService.getProfile(" USER@FINFLOW.COM ");

        assertEquals("Asha", result.getFirstName());
        assertEquals("Sharma", result.getLastName());
    }

    @Test
    void updateProfileShouldTrimAndPersistProvidedFields() {
        User user = sampleUser();
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFirstName(" Priya ");
        request.setLastName(" Mehta ");
        request.setDateOfBirth(LocalDate.now().minusYears(30));
        request.setPhoneNumber(" 9999999999 ");

        when(userRepository.findByEmail("user@finflow.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserResponse result = authService.updateProfile("user@finflow.com", request);

        assertEquals("Priya", result.getFirstName());
        assertEquals("Mehta", result.getLastName());
        assertEquals("9999999999", result.getPhoneNumber());
    }

    @Test
    void updateProfileShouldRejectUnderageApplicant() {
        User user = sampleUser();
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setDateOfBirth(LocalDate.now().minusYears(17));

        when(userRepository.findByEmail("user@finflow.com")).thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.updateProfile("user@finflow.com", request));

        assertEquals("You must be at least 18 years old to apply", exception.getMessage());
    }

    @Test
    void changePasswordShouldEncodeAndSaveNewPassword() {
        User user = sampleUser();
        user.setPassword("encoded-old");
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old");
        request.setNewPassword("new");

        when(userRepository.findByEmail("user@finflow.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encoded-old")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("encoded-new");

        assertEquals("Password changed successfully", authService.changePassword("user@finflow.com", request));
        assertEquals("encoded-new", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void changePasswordShouldRejectWrongCurrentPassword() {
        User user = sampleUser();
        user.setPassword("encoded-old");
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong");

        when(userRepository.findByEmail("user@finflow.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-old")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.changePassword("user@finflow.com", request));

        assertEquals("Invalid password", exception.getMessage());
    }

    private SignupRequest validSignupRequest() {
        SignupRequest request = new SignupRequest();
        request.setFirstName("Asha");
        request.setLastName("Sharma");
        request.setDateOfBirth(LocalDate.now().minusYears(24));
        request.setPhoneNumber("9876543210");
        request.setEmail(" user@finflow.com ");
        request.setPassword("secret");
        request.setReferralCode(" FRIEND10 ");
        return request;
    }

    private User sampleUser() {
        User user = new User();
        user.setId(2L);
        user.setEmail("user@finflow.com");
        user.setRole(" admin ");
        user.setFirstName("Asha");
        user.setLastName("Sharma");
        user.setDateOfBirth(LocalDate.now().minusYears(24));
        user.setPhoneNumber("9876543210");
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }
}
