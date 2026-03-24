package com.finflow.document_service.service;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.finflow.document_service.dto.DocumentResponseDTO;
import com.finflow.document_service.entity.Document;
import com.finflow.document_service.repository.DocumentRepository;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository repository;

    @Autowired
    private ModelMapper modelMapper;

    public DocumentResponseDTO save(MultipartFile file, Long applicationId) throws Exception {

        Document doc = new Document();
        doc.setFileName(file.getOriginalFilename());
        doc.setFileType(file.getContentType());
        doc.setData(file.getBytes());
        doc.setApplicationId(applicationId);

        Document saved = repository.save(doc);

        return modelMapper.map(saved, DocumentResponseDTO.class);
    }

    public Document get(Long id) {
        return repository.findById(id).orElseThrow();
    }
}