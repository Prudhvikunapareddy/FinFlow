package com.finflow.document_service.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

import com.finflow.document_service.dto.ApplicationOwnerResponseDTO;
import com.finflow.document_service.dto.DocumentResponseDTO;
import com.finflow.document_service.entity.Document;
import com.finflow.document_service.repository.DocumentRepository;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository repository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${application.service.url:http://APPLICATION-SERVICE}")
    private String applicationServiceUrl;

    public DocumentResponseDTO save(MultipartFile file, Long applicationId, String email, String role) throws Exception {
        validateApplicationAccess(applicationId, email, role);
        Document doc = new Document();
        doc.setFileName(file.getOriginalFilename());
        doc.setFileType(file.getContentType());
        doc.setData(file.getBytes());
        doc.setApplicationId(applicationId);
        doc.setUploadedByEmail(email);

        Document saved = repository.save(doc);

        return modelMapper.map(saved, DocumentResponseDTO.class);
    }

    public Document get(Long id, String email, String role) {
        Document document = repository.findById(id).orElseThrow();
        validateApplicationAccess(document.getApplicationId(), email, role);
        return document;
    }

    public boolean hasDocuments(Long applicationId) {
        return repository.existsByApplicationId(applicationId);
    }

    public List<DocumentResponseDTO> getByApplication(Long applicationId, String email, String role) {
        validateApplicationAccess(applicationId, email, role);
        return repository.findMetadataByApplicationId(applicationId);
    }

    private void validateApplicationAccess(Long applicationId, String email, String role) {
        if (isAdmin(role)) {
            return;
        }
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Authenticated user email is required");
        }

        ApplicationOwnerResponseDTO owner = restTemplate.getForObject(
                applicationServiceUrl + "/internal/applications/{id}/owner",
                ApplicationOwnerResponseDTO.class,
                applicationId);

        if (owner == null || owner.getApplicantName() == null) {
            throw new RuntimeException("Application owner could not be resolved");
        }

        if (!email.equals(owner.getApplicantName())) {
            throw new RuntimeException("Unauthorized");
        }
    }

    private boolean isAdmin(String role) {
        return role != null && "ADMIN".equalsIgnoreCase(role.trim());
    }
}
