package com.finflow.admin_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.finflow.admin_service.config.RabbitConfig;
import com.finflow.admin_service.dto.ApplicationStatusUpdateDTO;
import com.finflow.admin_service.dto.UserResponseDTO;
import com.finflow.admin_service.entity.Application;
import com.finflow.admin_service.repository.ApplicationRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private ApplicationRepository repository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminService, "documentServiceUrl", "http://DOCUMENT-SERVICE");
        ReflectionTestUtils.setField(adminService, "authServiceUrl", "http://AUTH-SERVICE");
    }

    @Test
    void getAllShouldReturnAllApplications() {
        when(repository.findAll()).thenReturn(List.of(new Application(), new Application()));

        assertEquals(2, adminService.getAll().size());
    }

    @Test
    void getByIdShouldReturnMatchingApplication() {
        Application application = new Application();
        application.setId(10L);

        when(repository.findById(10L)).thenReturn(Optional.of(application));

        assertEquals(10L, adminService.getById(10L).getId());
    }

    @Test
    void decisionShouldNormalizeStatusPersistAndPublishUpdate() {
        Application application = new Application();
        application.setId(5L);
        application.setStatus("DOCS_VERIFIED");

        when(repository.findById(5L)).thenReturn(Optional.of(application));
        when(repository.save(application)).thenReturn(application);

        Application result = adminService.decision(5L, " approved ");

        ArgumentCaptor<ApplicationStatusUpdateDTO> updateCaptor = ArgumentCaptor.forClass(ApplicationStatusUpdateDTO.class);
        verify(rabbitTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq(RabbitConfig.STATUS_UPDATE_QUEUE),
                updateCaptor.capture());

        assertEquals("APPROVED", result.getStatus());
        assertEquals(5L, updateCaptor.getValue().getId());
        assertEquals("APPROVED", updateCaptor.getValue().getStatus());
    }

    @Test
    void decisionShouldRejectUnsupportedStatus() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.decision(1L, "pending"));

        assertEquals("Invalid status: pending", exception.getMessage());
    }

    @Test
    void verifyDocumentShouldUpdateStatusAndPublishMessage() {
        Application application = new Application();
        application.setId(8L);
        application.setStatus("SUBMITTED");

        when(repository.findById(8L)).thenReturn(Optional.of(application));
        when(repository.save(application)).thenReturn(application);
        when(restTemplate.getForObject("http://DOCUMENT-SERVICE/documents/internal/applications/{id}/exists", Boolean.class, 8L))
                .thenReturn(true);

        String result = adminService.verifyDocument(8L);

        verify(repository).save(application);
        verify(rabbitTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq(RabbitConfig.STATUS_UPDATE_QUEUE), any(ApplicationStatusUpdateDTO.class));
        assertEquals("DOCS_VERIFIED", application.getStatus());
        assertEquals("Document verified. Application status updated to DOCS_VERIFIED.", result);
    }

    @Test
    void decisionShouldRejectApprovalBeforeDocumentVerification() {
        Application application = new Application();
        application.setId(5L);
        application.setStatus("SUBMITTED");

        when(repository.findById(5L)).thenReturn(Optional.of(application));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.decision(5L, "APPROVED"));

        assertEquals("Application can be approved only after documents are verified", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void verifyDocumentShouldRejectWhenNoDocumentsExist() {
        Application application = new Application();
        application.setId(8L);
        application.setStatus("SUBMITTED");

        when(repository.findById(8L)).thenReturn(Optional.of(application));
        when(restTemplate.getForObject("http://DOCUMENT-SERVICE/documents/internal/applications/{id}/exists", Boolean.class, 8L))
                .thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> adminService.verifyDocument(8L));

        assertEquals("No documents uploaded for application 8", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void getReportsShouldAggregateCounts() {
        when(repository.count()).thenReturn(9L);
        when(repository.countByStatus("SUBMITTED")).thenReturn(4L);
        when(repository.countByStatus("APPROVED")).thenReturn(2L);
        when(repository.countByStatus("REJECTED")).thenReturn(1L);
        when(repository.countByStatus("DOCS_VERIFIED")).thenReturn(3L);

        assertEquals(
                "Total Applications: 9, Submitted: 4, Approved: 2, Rejected: 1, Docs Verified: 3",
                adminService.getReports());
    }

    @Test
    void getUsersShouldReturnUsersFromAuthService() {
        List<UserResponseDTO> users = List.of(
                new UserResponseDTO(1L, "a@finflow.com", "USER"),
                new UserResponseDTO(2L, "b@finflow.com", "ADMIN"));

        when(restTemplate.exchange(
                org.mockito.ArgumentMatchers.eq("http://AUTH-SERVICE/internal/users"),
                org.mockito.ArgumentMatchers.eq(HttpMethod.GET),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<UserResponseDTO>>>any()))
                .thenReturn(ResponseEntity.ok(users));

        assertEquals(users, adminService.getUsers());
    }

    @Test
    void updateUserShouldNormalizeRoleAndReturnUpdatedUser() {
        UserResponseDTO updatedUser = new UserResponseDTO(3L, "manager@finflow.com", "ADMIN");

        when(restTemplate.exchange(
                org.mockito.ArgumentMatchers.eq("http://AUTH-SERVICE/internal/users/{id}/role"),
                org.mockito.ArgumentMatchers.eq(HttpMethod.PUT),
                org.mockito.ArgumentMatchers.<HttpEntity<UserResponseDTO>>any(),
                org.mockito.ArgumentMatchers.eq(UserResponseDTO.class),
                org.mockito.ArgumentMatchers.eq(3L)))
                .thenReturn(ResponseEntity.ok(updatedUser));

        UserResponseDTO result = adminService.updateUser(3L, " admin ");

        assertEquals(3L, result.getId());
        assertEquals("manager@finflow.com", result.getEmail());
        assertEquals("ADMIN", result.getRole());
    }

    @Test
    void updateUserShouldRejectBlankRole() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.updateUser(3L, " "));

        assertEquals("Role is required", exception.getMessage());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }
}
