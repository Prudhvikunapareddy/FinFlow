package com.finflow.application_service.dto;

public class ApplicationOwnerResponseDTO {
    private Long id;
    private String applicantName;
    private String status;

    public ApplicationOwnerResponseDTO() {
    }

    public ApplicationOwnerResponseDTO(Long id, String applicantName, String status) {
        this.id = id;
        this.applicantName = applicantName;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
