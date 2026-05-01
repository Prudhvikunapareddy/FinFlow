package com.finflow.application_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.finflow.application_service.config.RabbitConfig;
import com.finflow.application_service.dto.ApplicationMessageDTO;
import com.finflow.application_service.dto.ApplicationRequestDTO;
import com.finflow.application_service.dto.ApplicationResponseDTO;
import com.finflow.application_service.entity.LoanApplication;
import com.finflow.application_service.repository.ApplicationRepository;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private ApplicationRepository repository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    void createShouldSaveDraftApplicationForAuthenticatedUser() {
        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setName("Personal loan");
        request.setAmount(5000.0);
        request.setLoanType("PERSONAL");
        request.setTenureMonths(12);

        LoanApplication mapped = new LoanApplication();
        mapped.setName("Personal loan");
        mapped.setAmount(5000.0);

        LoanApplication saved = new LoanApplication();
        saved.setId(1L);
        saved.setName("Personal loan");
        saved.setApplicantName("user@finflow.com");
        saved.setAmount(5000.0);
        saved.setLoanType("PERSONAL");
        saved.setTenureMonths(12);
        saved.setStatus("DRAFT");

        ApplicationResponseDTO response = new ApplicationResponseDTO();
        response.setId(1L);
        response.setName("Personal loan");
        response.setApplicantName("user@finflow.com");
        response.setAmount(5000.0);
        response.setLoanType("PERSONAL");
        response.setTenureMonths(12);
        response.setStatus("DRAFT");

        when(modelMapper.map(request, LoanApplication.class)).thenReturn(mapped);
        when(repository.save(mapped)).thenReturn(saved);
        when(modelMapper.map(saved, ApplicationResponseDTO.class)).thenReturn(response);

        ApplicationResponseDTO result = applicationService.create(request, "user@finflow.com");

        assertEquals("DRAFT", mapped.getStatus());
        assertEquals("user@finflow.com", mapped.getApplicantName());
        assertEquals(1L, result.getId());
        verify(repository).save(mapped);
    }

    @Test
    void createShouldRejectBlankEmail() {
        ApplicationRequestDTO request = new ApplicationRequestDTO();

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> applicationService.create(request, " "));

        assertEquals("Authenticated user email is required", exception.getMessage());
    }

    @Test
    void getByIdShouldMapExistingApplication() {
        LoanApplication application = new LoanApplication();
        application.setId(3L);

        ApplicationResponseDTO response = new ApplicationResponseDTO();
        response.setId(3L);

        when(repository.findById(3L)).thenReturn(Optional.of(application));
        when(modelMapper.map(application, ApplicationResponseDTO.class)).thenReturn(response);

        ApplicationResponseDTO result = applicationService.getById(3L);

        assertEquals(3L, result.getId());
    }

    @Test
    void getByIdForUserShouldRejectDifferentApplicant() {
        LoanApplication application = new LoanApplication();
        application.setId(3L);
        application.setApplicantName("owner@finflow.com");

        when(repository.findById(3L)).thenReturn(Optional.of(application));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> applicationService.getByIdForUser(3L, "other@finflow.com"));

        assertEquals("Unauthorized", exception.getMessage());
    }

    @Test
    void updateShouldChangeAmountAndReturnMappedResponse() {
        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setName("Updated loan");
        request.setAmount(12000.0);
        request.setLoanType("HOME");
        request.setTenureMonths(24);

        LoanApplication existing = new LoanApplication();
        existing.setId(7L);
        existing.setName("Old loan");
        existing.setAmount(4000.0);
        existing.setStatus("DRAFT");

        LoanApplication updated = new LoanApplication();
        updated.setId(7L);
        updated.setAmount(12000.0);

        ApplicationResponseDTO response = new ApplicationResponseDTO();
        response.setId(7L);
        response.setAmount(12000.0);

        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(updated);
        when(modelMapper.map(updated, ApplicationResponseDTO.class)).thenReturn(response);

        ApplicationResponseDTO result = applicationService.update(7L, request);

        assertEquals(12000.0, existing.getAmount());
        assertEquals(12000.0, result.getAmount());
    }

    @Test
    void deleteShouldRemoveExistingApplication() {
        LoanApplication existing = new LoanApplication();
        existing.setId(4L);
        existing.setStatus("DRAFT");

        when(repository.findById(4L)).thenReturn(Optional.of(existing));

        applicationService.delete(4L);

        verify(repository).delete(existing);
    }

    @Test
    void getAllShouldMapAllApplications() {
        LoanApplication first = new LoanApplication();
        first.setId(1L);
        LoanApplication second = new LoanApplication();
        second.setId(2L);

        ApplicationResponseDTO firstResponse = new ApplicationResponseDTO();
        firstResponse.setId(1L);
        ApplicationResponseDTO secondResponse = new ApplicationResponseDTO();
        secondResponse.setId(2L);

        when(repository.findAll()).thenReturn(List.of(first, second));
        when(modelMapper.map(first, ApplicationResponseDTO.class)).thenReturn(firstResponse);
        when(modelMapper.map(second, ApplicationResponseDTO.class)).thenReturn(secondResponse);

        List<ApplicationResponseDTO> result = applicationService.getAll();

        assertEquals(2, result.size());
        assertEquals(2L, result.get(1).getId());
    }

    @Test
    void getAllForUserShouldDelegateToApplicantLookup() {
        LoanApplication application = new LoanApplication();
        application.setId(20L);

        ApplicationResponseDTO response = new ApplicationResponseDTO();
        response.setId(20L);

        when(repository.findByApplicantName("user@finflow.com")).thenReturn(List.of(application));
        when(modelMapper.map(application, ApplicationResponseDTO.class)).thenReturn(response);

        List<ApplicationResponseDTO> result = applicationService.getAllForUser("user@finflow.com");

        assertEquals(1, result.size());
        assertEquals(20L, result.getFirst().getId());
    }

    @Test
    void submitShouldUpdateStatusAndPublishMessage() {
        LoanApplication existing = new LoanApplication();
        existing.setId(9L);
        existing.setName("Personal loan");
        existing.setApplicantName("user@finflow.com");
        existing.setAmount(5000.0);
        existing.setLoanType("PERSONAL");
        existing.setTenureMonths(12);
        existing.setStatus("DRAFT");

        LoanApplication updated = new LoanApplication();
        updated.setId(9L);
        updated.setName("Personal loan");
        updated.setApplicantName("user@finflow.com");
        updated.setAmount(5000.0);
        updated.setLoanType("PERSONAL");
        updated.setTenureMonths(12);
        updated.setStatus("SUBMITTED");

        ApplicationResponseDTO response = new ApplicationResponseDTO();
        response.setId(9L);
        response.setStatus("SUBMITTED");

        when(repository.findById(9L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(updated);
        when(modelMapper.map(updated, ApplicationResponseDTO.class)).thenReturn(response);

        ApplicationResponseDTO result = applicationService.submit(9L, "user@finflow.com");

        ArgumentCaptor<ApplicationMessageDTO> messageCaptor = ArgumentCaptor.forClass(ApplicationMessageDTO.class);
        verify(rabbitTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq(RabbitConfig.QUEUE), messageCaptor.capture());

        ApplicationMessageDTO message = messageCaptor.getValue();
        assertNotNull(message);
        assertEquals(9L, message.getId());
        assertEquals("Personal loan", message.getName());
        assertEquals("user@finflow.com", message.getApplicantName());
        assertEquals("SUBMITTED", message.getStatus());
        assertEquals("SUBMITTED", existing.getStatus());
        assertEquals("SUBMITTED", result.getStatus());
    }

    @Test
    void submitShouldRejectDifferentApplicant() {
        LoanApplication existing = new LoanApplication();
        existing.setId(9L);
        existing.setApplicantName("owner@finflow.com");

        when(repository.findById(9L)).thenReturn(Optional.of(existing));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> applicationService.submit(9L, "other@finflow.com"));

        assertEquals("Unauthorized", exception.getMessage());
        verify(repository, never()).save(any());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void getMyApplicationsShouldUseApplicantEmailLookup() {
        LoanApplication application = new LoanApplication();
        application.setId(11L);
        

        ApplicationResponseDTO response = new ApplicationResponseDTO();
        response.setId(11L);

        when(repository.findByApplicantName("user@finflow.com")).thenReturn(List.of(application));
        when(modelMapper.map(application, ApplicationResponseDTO.class)).thenReturn(response);

        List<ApplicationResponseDTO> result = applicationService.getMyApplications("user@finflow.com");

        assertEquals(1, result.size());
        assertEquals(11L, result.getFirst().getId());
    }

    @Test
    void getStatusShouldReturnStoredStatus() {
        LoanApplication application = new LoanApplication();
        application.setId(15L);
        application.setStatus("APPROVED");

        when(repository.findById(15L)).thenReturn(Optional.of(application));

        assertEquals("APPROVED", applicationService.getStatus(15L));
    }

    @Test
    void getStatusForUserShouldRejectDifferentApplicant() {
        LoanApplication application = new LoanApplication();
        application.setId(15L);
        application.setApplicantName("owner@finflow.com");

        when(repository.findById(15L)).thenReturn(Optional.of(application));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> applicationService.getStatusForUser(15L, "other@finflow.com"));

        assertEquals("Unauthorized", exception.getMessage());
    }

    @Test
    void createShouldDefaultLoanTypeAndTenure() {
        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setName(" Starter loan ");
        request.setAmount(2500.0);

        LoanApplication mapped = new LoanApplication();
        LoanApplication saved = new LoanApplication();
        saved.setId(30L);
        ApplicationResponseDTO response = new ApplicationResponseDTO();
        response.setId(30L);

        when(modelMapper.map(request, LoanApplication.class)).thenReturn(mapped);
        when(repository.save(mapped)).thenReturn(saved);
        when(modelMapper.map(saved, ApplicationResponseDTO.class)).thenReturn(response);

        applicationService.create(request, "USER@FINFLOW.COM");

        assertEquals("Starter loan", mapped.getName());
        assertEquals("PERSONAL", mapped.getLoanType());
        assertEquals(12, mapped.getTenureMonths());
        assertEquals("user@finflow.com", mapped.getApplicantName());
    }

    @Test
    void updateForUserShouldPersistOwnedDraftApplication() {
        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setName("Business loan");
        request.setAmount(8000.0);
        request.setLoanType("BUSINESS");
        request.setTenureMonths(36);

        LoanApplication existing = new LoanApplication();
        existing.setId(31L);
        existing.setApplicantName("user@finflow.com");
        existing.setStatus("DRAFT");

        LoanApplication updated = new LoanApplication();
        updated.setId(31L);
        updated.setAmount(8000.0);

        ApplicationResponseDTO response = new ApplicationResponseDTO();
        response.setId(31L);
        response.setAmount(8000.0);

        when(repository.findById(31L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(updated);
        when(modelMapper.map(updated, ApplicationResponseDTO.class)).thenReturn(response);

        ApplicationResponseDTO result = applicationService.updateForUser(31L, request, "user@finflow.com");

        assertEquals("Business loan", existing.getName());
        assertEquals("BUSINESS", existing.getLoanType());
        assertEquals(36, existing.getTenureMonths());
        assertEquals(8000.0, result.getAmount());
    }

    @Test
    void deleteForUserShouldRemoveOwnedDraftApplication() {
        LoanApplication existing = new LoanApplication();
        existing.setId(32L);
        existing.setApplicantName("user@finflow.com");
        existing.setStatus("DRAFT");

        when(repository.findById(32L)).thenReturn(Optional.of(existing));

        applicationService.deleteForUser(32L, "user@finflow.com");

        verify(repository).delete(existing);
    }

    @Test
    void updateShouldRejectInvalidLoanType() {
        ApplicationRequestDTO request = new ApplicationRequestDTO();
        request.setName("Bad loan");
        request.setLoanType("crypto");

        LoanApplication existing = new LoanApplication();
        existing.setId(33L);
        existing.setStatus("DRAFT");

        when(repository.findById(33L)).thenReturn(Optional.of(existing));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> applicationService.update(33L, request));

        assertEquals("Invalid loan type: crypto", exception.getMessage());
    }

    @Test
    void getApplicationForInternalUseShouldReturnApplication() {
        LoanApplication application = new LoanApplication();
        application.setId(34L);

        when(repository.findById(34L)).thenReturn(Optional.of(application));

        assertEquals(34L, applicationService.getApplicationForInternalUse(34L).getId());
    }
}
