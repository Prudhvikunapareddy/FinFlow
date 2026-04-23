package com.finflow.application_service.service;

import java.util.List;
import java.util.Locale;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.finflow.application_service.dto.ApplicationRequestDTO;
import com.finflow.application_service.dto.ApplicationResponseDTO;
import com.finflow.application_service.entity.LoanApplication;
import com.finflow.application_service.repository.ApplicationRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.finflow.application_service.config.RabbitConfig;
import com.finflow.application_service.dto.ApplicationMessageDTO;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationRepository repository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public ApplicationResponseDTO create(ApplicationRequestDTO dto, String email) {
        String applicantEmail = requireEmail(email);

        LoanApplication app = modelMapper.map(dto, LoanApplication.class);
        app.setName(normalizeName(dto.getName()));
        app.setApplicantName(applicantEmail);
        app.setStatus("DRAFT");

        LoanApplication saved = repository.save(app);
        publishApplicationSnapshot(saved);

        return modelMapper.map(saved, ApplicationResponseDTO.class);
    }
    public ApplicationResponseDTO getById(Long id) {
        LoanApplication app = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        return modelMapper.map(app, ApplicationResponseDTO.class);
    }

    public ApplicationResponseDTO getByIdForUser(Long id, String email) {
        LoanApplication app = getOwnedApplication(id, email);
        return modelMapper.map(app, ApplicationResponseDTO.class);
    }
    
    public ApplicationResponseDTO update(Long id, ApplicationRequestDTO dto) {

        LoanApplication app = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        ensureDraft(app, "Only draft applications can be updated");
        app.setName(normalizeName(dto.getName()));
        app.setAmount(dto.getAmount());

        LoanApplication updated = repository.save(app);
        publishApplicationSnapshot(updated);

        return modelMapper.map(updated, ApplicationResponseDTO.class);
    }

    public ApplicationResponseDTO updateForUser(Long id, ApplicationRequestDTO dto, String email) {
        LoanApplication app = getOwnedApplication(id, email);
        ensureDraft(app, "Only draft applications can be updated");
        app.setName(normalizeName(dto.getName()));
        app.setAmount(dto.getAmount());
        LoanApplication updated = repository.save(app);
        publishApplicationSnapshot(updated);
        return modelMapper.map(updated, ApplicationResponseDTO.class);
    }

    public void delete(Long id) {

        LoanApplication app = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        ensureDraft(app, "Only draft applications can be deleted");
        repository.delete(app);
        publishApplicationDeletion(id);
    }

    public void deleteForUser(Long id, String email) {
        LoanApplication app = getOwnedApplication(id, email);
        ensureDraft(app, "Only draft applications can be deleted");
        repository.delete(app);
        publishApplicationDeletion(id);
    }

    public List<ApplicationResponseDTO> getAll() {
        return repository.findAll()
                .stream()
                .map(app -> modelMapper.map(app, ApplicationResponseDTO.class))
                .toList();
    }

    public List<ApplicationResponseDTO> getAllForUser(String email) {
        String applicantEmail = requireEmail(email);
        return getMyApplications(applicantEmail);
    }

    public ApplicationResponseDTO submit(Long id, String email) {
        String applicantEmail = requireEmail(email);
        LoanApplication app = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        if (!app.getApplicantName().equals(applicantEmail)) {
            throw new RuntimeException("Unauthorized");
        }
        ensureDraft(app, "Only draft applications can be submitted");
        app.setStatus("SUBMITTED");
        LoanApplication updated = repository.save(app);
        publishApplicationSnapshot(updated);

        return modelMapper.map(updated, ApplicationResponseDTO.class);
    }

    public List<ApplicationResponseDTO> getMyApplications(String email) {
        return repository.findByApplicantName(email)
                .stream()
                .map(app -> modelMapper.map(app, ApplicationResponseDTO.class))
                .toList();
    }

    public String getStatus(Long id) {
        LoanApplication app = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        return app.getStatus();
    }

    public String getStatusForUser(Long id, String email) {
        return getOwnedApplication(id, email).getStatus();
    }

    public LoanApplication getApplicationForInternalUse(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
    }

    private String requireEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Authenticated user email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private LoanApplication getOwnedApplication(Long id, String email) {
        String applicantEmail = requireEmail(email);
        LoanApplication app = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if (!applicantEmail.equals(app.getApplicantName())) {
            throw new RuntimeException("Unauthorized");
        }

        return app;
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new RuntimeException("Loan name is required");
        }
        return name.trim();
    }

    private void ensureDraft(LoanApplication app, String message) {
        if (!"DRAFT".equalsIgnoreCase(app.getStatus())) {
            throw new RuntimeException(message);
        }
    }

    private void publishApplicationSnapshot(LoanApplication app) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.QUEUE,
                ApplicationMessageDTO.builder()
                        .id(app.getId())
                        .name(app.getName())
                        .applicantName(app.getApplicantName())
                        .amount(app.getAmount())
                        .status(app.getStatus())
                        .action("UPSERT")
                        .build());
    }

    private void publishApplicationDeletion(Long id) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.QUEUE,
                ApplicationMessageDTO.builder()
                        .id(id)
                        .action("DELETE")
                        .build());
    }
}
