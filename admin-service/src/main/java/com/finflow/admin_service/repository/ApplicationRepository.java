package com.finflow.admin_service.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.finflow.admin_service.entity.Application;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            insert into application (id, name, applicant_name, amount, loan_type, tenure_months, status)
            values (:id, :name, :applicantName, :amount, :loanType, :tenureMonths, :status)
            on conflict (id) do update
            set name = excluded.name,
                applicant_name = excluded.applicant_name,
                amount = excluded.amount,
                loan_type = excluded.loan_type,
                tenure_months = excluded.tenure_months,
                status = excluded.status
            """, nativeQuery = true)
    void upsertApplication(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("applicantName") String applicantName,
            @Param("amount") Double amount,
            @Param("loanType") String loanType,
            @Param("tenureMonths") Integer tenureMonths,
            @Param("status") String status);

    long countByStatus(String status);
}
