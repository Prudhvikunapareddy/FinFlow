package com.finflow.document_service.dto;



import lombok.Data;

@Data
public class DocumentResponseDTO {

    private Long id;
    private String fileName;
    private String fileType;
    private Long applicationId;
}