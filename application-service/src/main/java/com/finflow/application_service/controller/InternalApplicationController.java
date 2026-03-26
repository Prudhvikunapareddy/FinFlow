package com.finflow.application_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finflow.application_service.dto.ApplicationOwnerResponseDTO;
import com.finflow.application_service.entity.LoanApplication;
import com.finflow.application_service.service.ApplicationService;

@RestController
@RequestMapping("/internal/applications")
public class InternalApplicationController {

    private final ApplicationService applicationService;

    public InternalApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/{id}/owner")
    public ApplicationOwnerResponseDTO getOwner(@PathVariable Long id) {
        LoanApplication app = applicationService.getApplicationForInternalUse(id);
        return new ApplicationOwnerResponseDTO(app.getId(), app.getApplicantName(), app.getStatus());
    }
}
