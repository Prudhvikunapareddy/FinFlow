package com.finflow.application_service.dto;

public class ApplicationStatusUpdateDTO {
    private Long id;
    private String status;
    private String adminNotes;
    private Boolean adminNotesUpdated;

    public ApplicationStatusUpdateDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAdminNotes() {
        return adminNotes;
    }

    public void setAdminNotes(String adminNotes) {
        this.adminNotes = adminNotes;
    }

    public Boolean getAdminNotesUpdated() {
        return adminNotesUpdated;
    }

    public void setAdminNotesUpdated(Boolean adminNotesUpdated) {
        this.adminNotesUpdated = adminNotesUpdated;
    }
}
