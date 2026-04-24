package com.finflow.document_service.dto;
public class DocumentResponseDTO {

    private Long id;
    private String fileName;
    private String fileType;
    private Long applicationId;

    public DocumentResponseDTO() {
    }

    public DocumentResponseDTO(Long id, String fileName, String fileType, Long applicationId) {
        this.id = id;
        this.fileName = fileName;
        this.fileType = fileType;
        this.applicationId = applicationId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }
}
