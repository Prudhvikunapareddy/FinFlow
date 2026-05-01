package com.finflow.admin_service.dto;

public class ApplicationStatusUpdateDTO {
    private Long id;
    private String status;
    private String adminNotes;
    private Boolean adminNotesUpdated;

    public ApplicationStatusUpdateDTO() {
    }

    public ApplicationStatusUpdateDTO(Long id, String status) {
        this.id = id;
        this.status = status;
        this.adminNotesUpdated = false;
    }

    public static ApplicationStatusUpdateDTO notesUpdate(Long id, String adminNotes) {
        ApplicationStatusUpdateDTO dto = new ApplicationStatusUpdateDTO();
        dto.setId(id);
        dto.setAdminNotes(adminNotes);
        dto.setAdminNotesUpdated(true);
        return dto;
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
