package com.finflow.auth_service.REPOSITORY;


import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.finflow.auth_service.Entity.User;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
