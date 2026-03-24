package com.finflow.application_service.dto;




import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplicationRequestDTO {

    @NotNull(message = "Amount is required")
    @Min(value = 1000, message = "Amount must be greater than 1000")
    private Double amount;
}