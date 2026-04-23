package com.finflow.auth_service.CONTROLLER;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finflow.auth_service.DTOs.RoleUpdateRequest;
import com.finflow.auth_service.DTOs.UserResponse;
import com.finflow.auth_service.SERVICE_LAYER.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    @Autowired
    private AuthService authService;

    @GetMapping
    public List<UserResponse> getUsers() {
        return authService.getUsers();
    }

    @PutMapping("/{id}/role")
    public UserResponse updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest request) {
        return authService.updateUserRole(id, request);
    }
}
