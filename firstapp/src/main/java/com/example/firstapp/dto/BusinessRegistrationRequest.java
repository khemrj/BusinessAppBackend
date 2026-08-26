package com.example.firstapp.dto;
import com.example.firstapp.enums.CompanySize;
import com.example.firstapp.enums.IndustryType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BusinessRegistrationRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank String companyName,
    @NotBlank String registrationNumber,
    @NotNull IndustryType industryType,
    CompanySize companySize,
    String website,
    String description,
    @Valid @NotNull AddressRequest headquarters,
    @NotBlank @Email String contactEmail,
    String contactPhone
) {}
