package com.finflow.document_service.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class UserHeaderRequestWrapper extends HttpServletRequestWrapper {

    private final String email;
    private final String role;

    public UserHeaderRequestWrapper(HttpServletRequest request, String email, String role) {
        super(request);
        this.email = email;
        this.role = role;
    }

    @Override
    public String getHeader(String name) {
        if ("X-User-Email".equalsIgnoreCase(name) && email != null && !email.isBlank()) {
            return email;
        }
        if ("X-User-Role".equalsIgnoreCase(name) && role != null && !role.isBlank()) {
            return role;
        }
        return super.getHeader(name);
    }
}
