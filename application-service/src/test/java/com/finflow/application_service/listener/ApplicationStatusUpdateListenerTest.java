package com.finflow.application_service.listener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.finflow.application_service.dto.ApplicationStatusUpdateDTO;
import com.finflow.application_service.entity.LoanApplication;
import com.finflow.application_service.repository.ApplicationRepository;

@ExtendWith(MockitoExtension.class)
class ApplicationStatusUpdateListenerTest {

    @Mock
    private ApplicationRepository repository;

    @Test
    void receiveShouldSyncAdminNotesWhenNotesUpdated() {
        LoanApplication application = new LoanApplication();
        application.setId(6L);
        application.setStatus("SUBMITTED");

        ApplicationStatusUpdateDTO message = new ApplicationStatusUpdateDTO();
        message.setId(6L);
        message.setAdminNotes("verified manually");
        message.setAdminNotesUpdated(true);

        when(repository.findById(6L)).thenReturn(Optional.of(application));

        new ApplicationStatusUpdateListener(repository).receive(message);

        assertEquals("SUBMITTED", application.getStatus());
        assertEquals("verified manually", application.getAdminNotes());
        verify(repository).save(application);
    }

    @Test
    void receiveShouldNotClearAdminNotesForStatusOnlyMessage() {
        LoanApplication application = new LoanApplication();
        application.setId(6L);
        application.setStatus("SUBMITTED");
        application.setAdminNotes("existing note");

        ApplicationStatusUpdateDTO message = new ApplicationStatusUpdateDTO();
        message.setId(6L);
        message.setStatus("APPROVED");

        when(repository.findById(6L)).thenReturn(Optional.of(application));

        new ApplicationStatusUpdateListener(repository).receive(message);

        assertEquals("APPROVED", application.getStatus());
        assertEquals("existing note", application.getAdminNotes());
        verify(repository).save(application);
    }
}
