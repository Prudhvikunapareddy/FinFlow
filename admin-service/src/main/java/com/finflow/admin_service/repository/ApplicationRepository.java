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
            insert into application (id, name, applicant_name, amount, status)
            values (:id, :name, :applicantName, :amount, :status)
            on conflict (id) do update
            set name = excluded.name,
                applicant_name = excluded.applicant_name,
                amount = excluded.amount,
                status = excluded.status
            """, nativeQuery = true)
    void upsertApplication(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("applicantName") String applicantName,
            @Param("amount") Double amount,
            @Param("status") String status);

    long countByStatus(String status);
}
