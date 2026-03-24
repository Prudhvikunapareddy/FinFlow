package com.finflow.application_service.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.finflow.application_service.dto.ApplicationRequestDTO;
import com.finflow.application_service.dto.ApplicationResponseDTO;
import com.finflow.application_service.service.ApplicationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    @Autowired
    private ApplicationService service;

    @PostMapping
    public ApplicationResponseDTO create(
            @Valid @RequestBody ApplicationRequestDTO dto,
            @RequestHeader(value = "X-User-Email" ,required = false) String email) {

        return service.create(dto, email);
    }

    @GetMapping
    public List<ApplicationResponseDTO> getAll() {
        return service.getAll();
    }
    
    @GetMapping("/{id}")
    public ApplicationResponseDTO getById(@PathVariable Long id) {
        return service.getById(id);
    }
    @PutMapping("/{id}")
    public ApplicationResponseDTO update(
            @PathVariable Long id,
            @RequestBody ApplicationRequestDTO dto) {

        return service.update(id, dto);
    }
    @DeleteMapping("/{id}")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "Application deleted successfully";
    }

    @PostMapping("/{id}/submit")
    public ApplicationResponseDTO submit(
            @PathVariable Long id, 
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        return service.submit(id, email);
    }

    @GetMapping("/my")
    public List<ApplicationResponseDTO> getMyApplications(
            @RequestHeader(value = "X-User-Email", required = true) String email) {
        return service.getMyApplications(email);
    }

    @GetMapping("/{id}/status")
    public String getStatus(@PathVariable Long id) {
        return service.getStatus(id);
    }

    @GetMapping("/test")
    public String test() {
        return "Application Service Working ";
    }
}