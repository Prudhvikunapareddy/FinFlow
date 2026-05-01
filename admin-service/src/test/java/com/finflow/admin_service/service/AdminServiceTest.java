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
    void getByIdShouldRejectMissingApplication() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> adminService.getById(99L));

        assertEquals("Not found", exception.getMessage());
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
    void decisionShouldRejectNullStatus() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.decision(5L, null));

        assertEquals("Status is required", exception.getMessage());
        verify(repository, never()).findById(any());
    }

    @Test
    void decisionShouldRejectRejectingDraftApplication() {
        Application application = new Application();
        application.setId(5L);
        application.setStatus("DRAFT");

        when(repository.findById(5L)).thenReturn(Optional.of(application));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.decision(5L, "REJECTED"));

        assertEquals("Only submitted applications can be rejected", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void decisionShouldRejectDirectDocsVerifiedStatus() {
        Application application = new Application();
        application.setId(5L);
        application.setStatus("SUBMITTED");

        when(repository.findById(5L)).thenReturn(Optional.of(application));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.decision(5L, "DOCS_VERIFIED"));

        assertEquals("Use the document verification endpoint to mark documents verified", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void verifyDocumentShouldRejectNonSubmittedApplication() {
        Application application = new Application();
        application.setId(8L);
        application.setStatus("DRAFT");

        when(repository.findById(8L)).thenReturn(Optional.of(application));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> adminService.verifyDocument(8L));

        assertEquals("Only submitted applications can be document-verified", exception.getMessage());
        verify(restTemplate, never()).getForObject(anyString(), org.mockito.ArgumentMatchers.eq(Boolean.class),
                org.mockito.ArgumentMatchers.anyLong());
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

    @Test
    void bulkDecisionShouldRejectEmptyIds() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.bulkDecision(List.of(), "REJECTED"));

        assertEquals("At least one application is required", exception.getMessage());
    }

    @Test
    void bulkDecisionShouldApplyDecisionToEveryApplication() {
        Application first = new Application();
        first.setId(11L);
        first.setStatus("SUBMITTED");
        Application second = new Application();
        second.setId(12L);
        second.setStatus("DOCS_VERIFIED");

        when(repository.findById(11L)).thenReturn(Optional.of(first));
        when(repository.findById(12L)).thenReturn(Optional.of(second));
        when(repository.save(first)).thenReturn(first);
        when(repository.save(second)).thenReturn(second);

        List<Application> result = adminService.bulkDecision(List.of(11L, 12L), "rejected");

        assertEquals(2, result.size());
        assertEquals("REJECTED", first.getStatus());
        assertEquals("REJECTED", second.getStatus());
        verify(rabbitTemplate, org.mockito.Mockito.times(2))
                .convertAndSend(org.mockito.ArgumentMatchers.eq(RabbitConfig.STATUS_UPDATE_QUEUE), any(ApplicationStatusUpdateDTO.class));
    }

    @Test
    void decisionShouldRejectFinalizedApplication() {
        Application application = new Application();
        application.setId(5L);
        application.setStatus("APPROVED");

        when(repository.findById(5L)).thenReturn(Optional.of(application));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.decision(5L, "REJECTED"));

        assertEquals("A final decision has already been made", exception.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void updateNotesShouldTrimAndPersistNotes() {
        Application application = new Application();
        application.setId(6L);

        when(repository.findById(6L)).thenReturn(Optional.of(application));
        when(repository.save(application)).thenReturn(application);

        Application result = adminService.updateNotes(6L, "  verified manually  ");

        ArgumentCaptor<ApplicationStatusUpdateDTO> updateCaptor = ArgumentCaptor.forClass(ApplicationStatusUpdateDTO.class);
        verify(rabbitTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq(RabbitConfig.STATUS_UPDATE_QUEUE),
                updateCaptor.capture());

        assertEquals("verified manually", result.getAdminNotes());
        assertEquals(6L, updateCaptor.getValue().getId());
        assertEquals("verified manually", updateCaptor.getValue().getAdminNotes());
        assertEquals(true, updateCaptor.getValue().getAdminNotesUpdated());
    }

    @Test
    void getUsersShouldReturnEmptyListWhenAuthServiceBodyIsNull() {
        when(restTemplate.exchange(
                org.mockito.ArgumentMatchers.eq("http://AUTH-SERVICE/internal/users"),
                org.mockito.ArgumentMatchers.eq(HttpMethod.GET),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.<ParameterizedTypeReference<List<UserResponseDTO>>>any()))
                .thenReturn(ResponseEntity.ok(null));

        assertEquals(List.of(), adminService.getUsers());
    }

    @Test
    void updateUserShouldRejectNullResponseBody() {
        when(restTemplate.exchange(
                org.mockito.ArgumentMatchers.eq("http://AUTH-SERVICE/internal/users/{id}/role"),
                org.mockito.ArgumentMatchers.eq(HttpMethod.PUT),
                org.mockito.ArgumentMatchers.<HttpEntity<UserResponseDTO>>any(),
                org.mockito.ArgumentMatchers.eq(UserResponseDTO.class),
                org.mockito.ArgumentMatchers.eq(3L)))
                .thenReturn(ResponseEntity.ok(null));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> adminService.updateUser(3L, "admin"));

        assertEquals("User update failed", exception.getMessage());
    }
}
