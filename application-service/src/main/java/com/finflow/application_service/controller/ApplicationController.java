package com.finflow.application_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finflow.application_service.entity.LoanApplication;
import com.finflow.application_service.service.ApplicationService;

@RestController
@RequestMapping("/applications")
public class ApplicationController {

    @Autowired
    private ApplicationService service;

    @PostMapping
    public LoanApplication create(@RequestBody LoanApplication app) {
        return service.create(app);
    }

    @GetMapping
    public List<LoanApplication> getAll() {
        return service.getAll();
    }

    @GetMapping("/test")
    public String test() {
        return "Application Service Working ";
    }
}
