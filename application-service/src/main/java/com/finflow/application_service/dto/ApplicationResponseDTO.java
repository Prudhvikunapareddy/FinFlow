package com.finflow.application_service.dto;



import lombok.Data;

@Data
public class ApplicationResponseDTO {

    private Long id;
    private String applicantName;
    private Double amount;
    private String status;
}