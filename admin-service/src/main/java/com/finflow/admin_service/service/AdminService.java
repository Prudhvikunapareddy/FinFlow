package com.finflow.admin_service.service;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.finflow.admin_service.entity.Application;
import com.finflow.admin_service.repository.ApplicationRepository;

@Service
public class AdminService {

    @Autowired
    private ApplicationRepository repository;

    // Get all applications
    public List<Application> getAll() {
        return repository.findAll();
    }

    // Get by ID
    public Application getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
    }

    public Application decision(Long id, String status) {
        Application app = getById(id);
        app.setStatus(status); // APPROVED or REJECTED
        return repository.save(app);
    }

    public String verifyDocument(Long id) {
        // Placeholder for inter-service call to verify document, or update local Application state
        Application app = getById(id);
        app.setStatus("DOCS_VERIFIED");
        repository.save(app);
        return "Document verified. Application status updated to DOCS_VERIFIED.";
    }

    public String getReports() {
        long total = repository.count();
        return "Total Applications: " + total;
    }

    public List<String> getUsers() {
        return List.of("admin", "applicant1", "applicant2");
    }

    public String updateUser(Long id, String role) {
        return "User " + id + " updated to role: " + role;
    }
}