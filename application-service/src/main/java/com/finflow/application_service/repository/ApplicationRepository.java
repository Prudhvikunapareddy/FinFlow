package com.finflow.application_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finflow.application_service.entity.LoanApplication;

public interface ApplicationRepository extends JpaRepository<LoanApplication, Long> {
}
