package com.finflow.document_service.entity;



import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileType;

    @Lob
    private byte[] data;

    private Long applicationId;
    private String uploadedByEmail;
}
