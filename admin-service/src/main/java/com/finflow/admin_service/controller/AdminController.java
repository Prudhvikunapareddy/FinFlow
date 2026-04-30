package com.finflow.admin_service.controller;





import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finflow.admin_service.dto.DecisionRequestDTO;
import com.finflow.admin_service.dto.AdminNotesRequestDTO;
import com.finflow.admin_service.dto.BulkDecisionRequestDTO;
import com.finflow.admin_service.dto.UserResponseDTO;
import com.finflow.admin_service.dto.UserUpdateDTO;
import com.finflow.admin_service.entity.Application;
import com.finflow.admin_service.service.AdminService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    @Autowired
    private AdminService service;

    @GetMapping("/applications")
    public List<Application> getAll() {
        return service.getAll();
    }

    @GetMapping("/applications/{id}")
    public Application getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping("/applications/{id}/decision")
    public Application decision(@PathVariable Long id, @Valid @RequestBody DecisionRequestDTO payload) {
        String status = payload.getStatus();
        return service.decision(id, status);
    }

    @PostMapping("/applications/bulk-decision")
    public List<Application> bulkDecision(@Valid @RequestBody BulkDecisionRequestDTO payload) {
        return service.bulkDecision(payload.getIds(), payload.getStatus());
    }

    @PutMapping("/applications/{id}/notes")
    public Application updateNotes(@PathVariable Long id, @RequestBody AdminNotesRequestDTO payload) {
        return service.updateNotes(id, payload.getNotes());
    }

    @PutMapping("/documents/{id}/verify")
    public String verifyDocument(@PathVariable Long id) {
        return service.verifyDocument(id);
    }

    @GetMapping("/reports")
    public String getReports() {
        return service.getReports();
    }

    @GetMapping("/users")
    public List<UserResponseDTO> getUsers() {
        return service.getUsers();
    }

    @PutMapping("/users/{id}")
    public UserResponseDTO updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO payload) {
        return service.updateUser(id, payload.getRole());
    }
}
