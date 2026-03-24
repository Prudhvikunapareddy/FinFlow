package com.finflow.application_service.service;

import java.util.List;

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

        LoanApplication app = modelMapper.map(dto, LoanApplication.class);

        app.setApplicantName(email);
        app.setStatus("DRAFT");

        LoanApplication saved = repository.save(app);

        return modelMapper.map(saved, ApplicationResponseDTO.class);
    }
    public ApplicationResponseDTO getById(Long id) {

        LoanApplication app = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        return modelMapper.map(app, ApplicationResponseDTO.class);
    }
    
    public ApplicationResponseDTO update(Long id, ApplicationRequestDTO dto) {

        LoanApplication app = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        app.setAmount(dto.getAmount());

        LoanApplication updated = repository.save(app);

        return modelMapper.map(updated, ApplicationResponseDTO.class);
    }
    public void delete(Long id) {

        LoanApplication app = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));

        repository.delete(app);
    }

    public List<ApplicationResponseDTO> getAll() {
        return repository.findAll()
                .stream()
                .map(app -> modelMapper.map(app, ApplicationResponseDTO.class))
                .toList();
    }

    public ApplicationResponseDTO submit(Long id, String email) {
        LoanApplication app = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        if(email != null && !app.getApplicantName().equals(email)) {
            throw new RuntimeException("Unauthorized");
        }
        app.setStatus("SUBMITTED");
        LoanApplication updated = repository.save(app);

        // Send to Admin Service queue
        ApplicationMessageDTO message = ApplicationMessageDTO.builder()
                .id(updated.getId())
                .name(updated.getApplicantName())
                .status(updated.getStatus())
                .build();
        rabbitTemplate.convertAndSend(RabbitConfig.QUEUE, message);

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
    
}