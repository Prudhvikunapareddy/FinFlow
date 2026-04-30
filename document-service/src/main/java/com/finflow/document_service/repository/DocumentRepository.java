package com.finflow.document_service.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.finflow.document_service.dto.DocumentResponseDTO;
import com.finflow.document_service.entity.Document;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    boolean existsByApplicationId(Long applicationId);

    @Query("""
            select new com.finflow.document_service.dto.DocumentResponseDTO(
                d.id,
                d.fileName,
                d.fileType,
                d.documentType,
                d.applicationId
            )
            from Document d
            where d.applicationId = :applicationId
            """)
    List<DocumentResponseDTO> findMetadataByApplicationId(Long applicationId);
}
