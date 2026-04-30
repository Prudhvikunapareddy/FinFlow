package com.finflow.auth_service.CONTROLLER;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
    public UserResponse profile(@RequestHeader("X-User-Email") String email) {
        return authService.getProfile(email);
    }

    @PutMapping("/profile")
    public UserResponse updateProfile(@RequestHeader("X-User-Email") String email, @Valid @RequestBody ProfileUpdateRequest request) {
        return authService.updateProfile(email, request);
    }

    @PutMapping("/password")
    public String changePassword(@RequestHeader("X-User-Email") String email, @Valid @RequestBody ChangePasswordRequest request) {
        return authService.changePassword(email, request);
    }
}
