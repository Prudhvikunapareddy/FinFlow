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
import com.finflow.admin_service.dto.UserUpdateDTO;
import com.finflow.admin_service.entity.Application;
import com.finflow.admin_service.service.AdminService;

@RestController
@RequestMapping("/admin")
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
    public Application decision(@PathVariable Long id, @RequestBody DecisionRequestDTO payload) {
        String status = payload.getStatus();
        return service.decision(id, status);
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
    public List<String> getUsers() {
        return service.getUsers();
    }

    @PutMapping("/users/{id}")
    public String updateUser(@PathVariable Long id, @RequestBody UserUpdateDTO payload) {
        return service.updateUser(id, payload.getRole());
    }
}