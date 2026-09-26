package com.example.firstapp.dto;
import com.example.firstapp.enums.CompanySize;
import com.example.firstapp.enums.IndustryType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
//business register garda halnu parne data haru

public record BusinessRegistrationRequest(
   
    @NotBlank String companyName,
    String registrationNumber,
    @NotNull IndustryType industryType,
    @NotNull CompanySize companySize,
    String website,
    String description,
    @Valid AddressRequest headquarters,
    @Email String contactEmail,
    String contactPhone
) {}
