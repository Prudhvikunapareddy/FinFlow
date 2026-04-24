package com.finflow.document_service.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.finflow.document_service.dto.ApplicationOwnerResponseDTO;
import com.finflow.document_service.dto.DocumentResponseDTO;
import com.finflow.document_service.entity.Document;
import com.finflow.document_service.repository.DocumentRepository;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository repository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(documentService, "applicationServiceUrl", "http://APPLICATION-SERVICE");
    }

    @Test
    void saveShouldPersistUploadedDocumentAndMapResponse() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "terms.pdf",
                "application/pdf",
                "hello".getBytes());

        Document saved = new Document();
        saved.setId(12L);
        saved.setFileName("terms.pdf");
        saved.setFileType("application/pdf");
        saved.setApplicationId(99L);

        DocumentResponseDTO response = new DocumentResponseDTO();
        response.setId(12L);
        response.setFileName("terms.pdf");
        response.setFileType("application/pdf");
        response.setApplicationId(99L);

        ApplicationOwnerResponseDTO owner = new ApplicationOwnerResponseDTO();
        owner.setId(99L);
        owner.setApplicantName("user@finflow.com");

        when(restTemplate.getForObject("http://APPLICATION-SERVICE/internal/applications/{id}/owner",
                ApplicationOwnerResponseDTO.class, 99L)).thenReturn(owner);
        when(repository.save(org.mockito.ArgumentMatchers.any(Document.class))).thenReturn(saved);
        when(modelMapper.map(saved, DocumentResponseDTO.class)).thenReturn(response);

        DocumentResponseDTO result = documentService.save(file, 99L, "user@finflow.com", "USER");

        org.mockito.ArgumentCaptor<Document> captor = org.mockito.ArgumentCaptor.forClass(Document.class);
        verify(repository).save(captor.capture());
        Document persisted = captor.getValue();

        assertEquals("terms.pdf", persisted.getFileName());
        assertEquals("application/pdf", persisted.getFileType());
        assertEquals(99L, persisted.getApplicationId());
        assertEquals("user@finflow.com", persisted.getUploadedByEmail());
        assertArrayEquals("hello".getBytes(), persisted.getData());
        assertEquals(12L, result.getId());
    }

    @Test
    void getShouldReturnStoredDocumentForOwner() {
        Document document = new Document();
        document.setId(2L);
        document.setApplicationId(99L);

        ApplicationOwnerResponseDTO owner = new ApplicationOwnerResponseDTO();
        owner.setId(99L);
        owner.setApplicantName("user@finflow.com");

        when(repository.findById(2L)).thenReturn(Optional.of(document));
        when(restTemplate.getForObject("http://APPLICATION-SERVICE/internal/applications/{id}/owner",
                ApplicationOwnerResponseDTO.class, 99L)).thenReturn(owner);

        assertSame(document, documentService.get(2L, "user@finflow.com", "USER"));
    }

    @Test
    void getShouldRejectDifferentUser() {
        Document document = new Document();
        document.setId(2L);
        document.setApplicationId(99L);

        ApplicationOwnerResponseDTO owner = new ApplicationOwnerResponseDTO();
        owner.setId(99L);
        owner.setApplicantName("owner@finflow.com");

        when(repository.findById(2L)).thenReturn(Optional.of(document));
        when(restTemplate.getForObject("http://APPLICATION-SERVICE/internal/applications/{id}/owner",
                ApplicationOwnerResponseDTO.class, 99L)).thenReturn(owner);

        RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> documentService.get(2L, "other@finflow.com", "USER"));

        assertEquals("Unauthorized", exception.getMessage());
    }

    @Test
    void getByApplicationShouldReturnDocumentMetadataForAdmin() {
        DocumentResponseDTO response = new DocumentResponseDTO(8L, "salary-slip.pdf", "application/pdf", 42L);
        when(repository.findMetadataByApplicationId(42L)).thenReturn(List.of(response));

        List<DocumentResponseDTO> result = documentService.getByApplication(42L, null, "ADMIN");

        assertEquals(1, result.size());
        assertEquals("salary-slip.pdf", result.get(0).getFileName());
    }
}
