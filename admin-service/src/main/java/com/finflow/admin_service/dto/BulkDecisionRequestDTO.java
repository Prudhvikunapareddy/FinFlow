package com.finflow.admin_service.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;

public class BulkDecisionRequestDTO {
    @NotEmpty(message = "At least one application is required")
    private List<Long> ids;

    @NotBlank(message = "Status is required")
    private String status;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
