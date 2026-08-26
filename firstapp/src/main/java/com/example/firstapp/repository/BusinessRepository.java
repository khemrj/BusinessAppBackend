package com.example.firstapp.repository;

import com.example.firstapp.entity.Business;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BusinessRepository extends JpaRepository<Business, Long> {

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByContactEmail(String contactEmail);

    Optional<Business> findByOwner_Id(Long userId);
}