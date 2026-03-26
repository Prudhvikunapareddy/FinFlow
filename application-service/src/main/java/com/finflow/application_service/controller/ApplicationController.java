package com.finflow.application_service.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.finflow.application_service.dto.ApplicationRequestDTO;
import com.finflow.application_service.dto.ApplicationResponseDTO;
import com.finflow.application_service.service.ApplicationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/applications")
@SecurityRequirement(name = "bearerAuth")
public class ApplicationController {

    @Autowired
    private ApplicationService service;

    @PostMapping
    public ApplicationResponseDTO create(
            @Valid @RequestBody ApplicationRequestDTO dto,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Email" ,required = false) String email) {

        return service.create(dto, email);
    }

    @GetMapping
    public List<ApplicationResponseDTO> getAll(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        return service.getAllForUser(email);
    }
    
    @GetMapping("/{id}")
    public ApplicationResponseDTO getById(
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        return service.getByIdForUser(id, email);
    }

    @PutMapping("/{id}")
    public ApplicationResponseDTO update(
            @PathVariable Long id,
            @RequestBody ApplicationRequestDTO dto,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Email", required = false) String email) {

        return service.updateForUser(id, dto, email);
    }

    @DeleteMapping("/{id}")
    public String delete(
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        service.deleteForUser(id, email);
        return "Application deleted successfully";
    }

    @PostMapping("/{id}/submit")
    public ApplicationResponseDTO submit(
            @PathVariable Long id, 
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        return service.submit(id, email);
    }

    @GetMapping("/my")
    public List<ApplicationResponseDTO> getMyApplications(
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Email", required = true) String email) {
        return service.getMyApplications(email);
    }

    @GetMapping("/{id}/status")
    public String getStatus(
            @PathVariable Long id,
            @Parameter(hidden = true)
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        return service.getStatusForUser(id, email);
    }

    @GetMapping("/test")
    @Operation(security = {})
    public String test() {
        return "Application Service Working ";
    }
}
