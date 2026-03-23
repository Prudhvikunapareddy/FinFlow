package com.finflow.auth_service.DTOs;

import lombok.Data;

@Data
public class SignupRequest {
    private String email;
    private String password;
}