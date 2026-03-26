package com.finflow.application_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApplicationOwnerResponseDTO {
    private Long id;
    private String applicantName;
    private String status;
}
