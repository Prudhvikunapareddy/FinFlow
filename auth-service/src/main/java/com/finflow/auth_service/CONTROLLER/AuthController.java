package com.finflow.auth_service.CONTROLLER;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.finflow.auth_service.DTOs.LoginRequest;
import com.finflow.auth_service.DTOs.ChangePasswordRequest;
import com.finflow.auth_service.DTOs.ProfileUpdateRequest;
import com.finflow.auth_service.DTOs.SignupRequest;
import com.finflow.auth_service.DTOs.UserResponse;
import com.finflow.auth_service.SERVICE_LAYER.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/signup")
    public String signup(@Valid @RequestBody SignupRequest request) {
        return authService.signup(request);
    }

    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/profile")
    public UserResponse profile(@RequestHeader(value = "X-User-Email", required = false) String email,
                                Authentication authentication) {
        return authService.getProfile(resolveEmail(email, authentication));
    }

    @PutMapping("/profile")
    public UserResponse updateProfile(@RequestHeader(value = "X-User-Email", required = false) String email,
                                      Authentication authentication,
                                      @Valid @RequestBody ProfileUpdateRequest request) {
        return authService.updateProfile(resolveEmail(email, authentication), request);
    }

    @PutMapping("/password")
    public String changePassword(@RequestHeader(value = "X-User-Email", required = false) String email,
                                 Authentication authentication,
                                 @Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(resolveEmail(email, authentication), request);
    }

    private String resolveEmail(String headerEmail, Authentication authentication) {
        if (headerEmail != null && !headerEmail.isBlank()) {
            return headerEmail;
        }
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        throw new RuntimeException("Authenticated user email is required");
    }
}
