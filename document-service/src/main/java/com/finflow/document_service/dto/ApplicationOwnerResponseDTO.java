package com.finflow.document_service.dto;

import lombok.Data;

@Data
public class ApplicationOwnerResponseDTO {
    private Long id;
    private String applicantName;
    private String status;
}
