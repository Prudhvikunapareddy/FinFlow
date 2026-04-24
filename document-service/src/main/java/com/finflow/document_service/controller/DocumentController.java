package com.finflow.document_service.controller;



import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.finflow.document_service.dto.DocumentResponseDTO;
import com.finflow.document_service.entity.Document;
import com.finflow.document_service.service.DocumentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/documents")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    @Autowired
    private DocumentService service;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DocumentResponseDTO upload(
            @Parameter(description = "Document file to upload")
            @RequestPart("file") MultipartFile file,
            @RequestParam("applicationId") Long applicationId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role) throws Exception {

        return service.save(file, applicationId, email, role);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        Document doc = service.get(id, email, role);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(doc.getData());
    }

    @GetMapping("/applications/{applicationId}/exists")
    public boolean hasDocuments(
            @PathVariable Long applicationId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (role == null || !"ADMIN".equalsIgnoreCase(role.trim())) {
            throw new RuntimeException("Admin role required");
        }
        return service.hasDocuments(applicationId);
    }

    @GetMapping("/internal/applications/{applicationId}/exists")
    public boolean hasDocumentsInternal(@PathVariable Long applicationId) {
        return service.hasDocuments(applicationId);
    }

    @GetMapping("/applications/{applicationId}")
    public List<DocumentResponseDTO> getByApplication(
            @PathVariable Long applicationId,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Email", required = false) String email,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        return service.getByApplication(applicationId, email, role);
    }

    @GetMapping("/test")
    @Operation(security = {})
    public String test() {
        return "Document Service Working";
    }
}
