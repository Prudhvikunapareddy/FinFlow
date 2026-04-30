package com.finflow.auth_service.SERVICE_LAYER;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.finflow.auth_service.DTOs.LoginRequest;
import com.finflow.auth_service.DTOs.SignupRequest;
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
}
