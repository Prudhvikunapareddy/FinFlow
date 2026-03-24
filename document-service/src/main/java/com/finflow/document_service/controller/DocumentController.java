package com.finflow.document_service.controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.finflow.document_service.dto.DocumentResponseDTO;
import com.finflow.document_service.entity.Document;
import com.finflow.document_service.service.DocumentService;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    @Autowired
    private DocumentService service;

    @PostMapping("/upload")
    public DocumentResponseDTO upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("applicationId") Long applicationId) throws Exception {

        return service.save(file, applicationId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable Long id) {

        Document doc = service.get(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(doc.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(doc.getData());
    }
    @GetMapping("/test")
    public String test() {
        return "Document Service Working";
    }
}