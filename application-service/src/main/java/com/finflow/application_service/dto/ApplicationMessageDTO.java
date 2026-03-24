package com.finflow.application_service.dto;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class ApplicationMessageDTO {
    private Long id;
    private String name;
    private String status;
}
