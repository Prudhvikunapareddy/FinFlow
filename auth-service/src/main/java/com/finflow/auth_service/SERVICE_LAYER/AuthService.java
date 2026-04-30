package com.finflow.auth_service.SERVICE_LAYER;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.finflow.auth_service.DTOs.LoginRequest;
import com.finflow.auth_service.DTOs.ChangePasswordRequest;
import com.finflow.auth_service.DTOs.ProfileUpdateRequest;
import com.finflow.auth_service.DTOs.RoleUpdateRequest;
import com.finflow.auth_service.DTOs.SignupRequest;
import com.finflow.auth_service.DTOs.UserResponse;
import com.finflow.auth_service.Entity.User;
import com.finflow.auth_service.REPOSITORY.UserRepository;
import com.finflow.auth_service.UTIL.JwtUtil;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    public String signup(SignupRequest request) {
        String email = normalizeEmail(request.getEmail());

    	if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already registered!");
        }
        if (request.getDateOfBirth().isAfter(LocalDate.now().minusYears(18))) {
            throw new RuntimeException("You must be at least 18 years old to apply");
        }

        User user = new User();
        user.setEmail(email);
        user.setFirstName(request.getFirstName().trim());
        user.setLastName(request.getLastName().trim());
        user.setDateOfBirth(request.getDateOfBirth());
        user.setPhoneNumber(request.getPhoneNumber().trim());
        user.setReferralCode(normalizeOptional(request.getReferralCode()));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setCreatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        return jwtUtil.generateToken(saved.getEmail(), saved.getRole());
    }

    public String login(LoginRequest request) {

        String email = normalizeEmail(request.getEmail());

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        return jwtUtil.generateToken(user.getEmail(), user.getRole().trim().toUpperCase(Locale.ROOT));
    }

    public List<UserResponse> getUsers() {
        return userRepository.findAllByOrderByIdAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse updateUserRole(Long id, RoleUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(normalizeRole(request.getRole()));
        return toResponse(userRepository.save(user));
    }

    public UserResponse getProfile(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return toResponse(user);
    }

    public UserResponse updateProfile(String email, ProfileUpdateRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (request.getDateOfBirth() != null && request.getDateOfBirth().isAfter(LocalDate.now().minusYears(18))) {
            throw new RuntimeException("You must be at least 18 years old to apply");
        }
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName().trim());
        if (request.getLastName() != null) user.setLastName(request.getLastName().trim());
        if (request.getDateOfBirth() != null) user.setDateOfBirth(request.getDateOfBirth());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber().trim());
        return toResponse(userRepository.save(user));
    }

    public String changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return "Password changed successfully";
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), normalizeRole(user.getRole()), user.getFirstName(), user.getLastName(), user.getDateOfBirth(), user.getPhoneNumber(), user.getCreatedAt());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            throw new RuntimeException("Role is required");
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (!"USER".equals(normalized) && !"ADMIN".equals(normalized)) {
            throw new RuntimeException("Invalid role: " + role);
        }
        return normalized;
    }
}
