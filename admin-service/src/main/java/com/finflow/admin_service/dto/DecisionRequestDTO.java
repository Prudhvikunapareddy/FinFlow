package com.finflow.admin_service.dto;

import jakarta.validation.constraints.NotBlank;

public class DecisionRequestDTO {
    @NotBlank(message = "Status is required")
    private String status;
    private String comments;

    public DecisionRequestDTO() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
