package com.finflow.application_service.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.finflow.application_service.entity.LoanApplication;
import com.finflow.application_service.repository.ApplicationRepository;

@Service
public class ApplicationService {

    @Autowired
    private ApplicationRepository repository;

    public LoanApplication create(LoanApplication app) {
        app.setStatus("DRAFT");
        return repository.save(app);
    }

    public List<LoanApplication> getAll() {
        return repository.findAll();
    }
}
