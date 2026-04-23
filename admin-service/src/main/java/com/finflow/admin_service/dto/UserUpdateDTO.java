package com.finflow.admin_service.dto;

import jakarta.validation.constraints.NotBlank;

public class UserUpdateDTO {
    @NotBlank(message = "Role is required")
    private String role;

    public UserUpdateDTO() {
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
