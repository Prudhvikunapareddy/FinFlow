package com.finflow.admin_service.service;


import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.finflow.admin_service.config.RabbitConfig;
import com.finflow.admin_service.dto.ApplicationStatusUpdateDTO;
import com.finflow.admin_service.entity.Application;
import com.finflow.admin_service.repository.ApplicationRepository;

@Service
public class AdminService {

    @Autowired
    private ApplicationRepository repository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${document.service.url:http://DOCUMENT-SERVICE}")
    private String documentServiceUrl;

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
        String normalizedStatus = normalizeStatus(status);
        Application app = getById(id);
        validateDecision(app, normalizedStatus);
        app.setStatus(normalizedStatus);
        Application updated = repository.save(app);
        publishStatusUpdate(updated.getId(), updated.getStatus());
        return updated;
    }

    public String verifyDocument(Long id) {
        Application app = getById(id);
        if (!"SUBMITTED".equals(app.getStatus())) {
            throw new RuntimeException("Only submitted applications can be document-verified");
        }
        Boolean hasDocuments = restTemplate.getForObject(
                documentServiceUrl + "/documents/internal/applications/{id}/exists",
                Boolean.class,
                id);
        if (!Boolean.TRUE.equals(hasDocuments)) {
            throw new RuntimeException("No documents uploaded for application " + id);
        }
        app.setStatus("DOCS_VERIFIED");
        repository.save(app);
        publishStatusUpdate(app.getId(), app.getStatus());
        return "Document verified. Application status updated to DOCS_VERIFIED.";
    }

    public String getReports() {
        long total = repository.count();
        long submitted = repository.countByStatus("SUBMITTED");
        long approved = repository.countByStatus("APPROVED");
        long rejected = repository.countByStatus("REJECTED");
        long docsVerified = repository.countByStatus("DOCS_VERIFIED");
        return "Total Applications: " + total
                + ", Submitted: " + submitted
                + ", Approved: " + approved
                + ", Rejected: " + rejected
                + ", Docs Verified: " + docsVerified;
    }

    public List<String> getUsers() {
        return repository.findDistinctApplicantNames();
    }

    public String updateUser(Long id, String role) {
        if (role == null || role.isBlank()) {
            throw new RuntimeException("Role is required");
        }
        return "User management is handled by auth-service. No local user record exists for id " + id
                + "; requested role was: " + role.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new RuntimeException("Status is required");
        }

        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("APPROVED", "REJECTED", "SUBMITTED", "DOCS_VERIFIED", "DRAFT").contains(normalizedStatus)) {
            throw new RuntimeException("Invalid status: " + status);
        }
        return normalizedStatus;
    }

    private void validateDecision(Application app, String normalizedStatus) {
        if ("APPROVED".equals(normalizedStatus) && !"DOCS_VERIFIED".equals(app.getStatus())) {
            throw new RuntimeException("Application can be approved only after documents are verified");
        }

        if ("DOCS_VERIFIED".equals(normalizedStatus)) {
            throw new RuntimeException("Use the document verification endpoint to mark documents verified");
        }
    }

    private void publishStatusUpdate(Long id, String status) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.STATUS_UPDATE_QUEUE,
                new ApplicationStatusUpdateDTO(id, status));
    }
}
