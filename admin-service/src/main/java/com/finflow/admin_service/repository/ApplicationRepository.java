package com.finflow.admin_service.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import com.finflow.admin_service.entity.Application;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
}