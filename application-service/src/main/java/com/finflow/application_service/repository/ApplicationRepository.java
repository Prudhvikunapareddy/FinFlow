package com.finflow.application_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import com.finflow.application_service.entity.LoanApplication;

public interface ApplicationRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findByApplicantName(String applicantName);
}
