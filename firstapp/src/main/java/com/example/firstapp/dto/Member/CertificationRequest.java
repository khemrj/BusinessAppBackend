package com.example.firstapp.dto.Member;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


// dto/request/CertificationRequest.java
public record CertificationRequest(
    @NotBlank @Size(max = 150) String name,
    @NotBlank @Size(max = 150) String issuingOrganization,
    @NotNull LocalDate issuedDate,
    LocalDate expiryDate,
    String credentialUrl){
} 