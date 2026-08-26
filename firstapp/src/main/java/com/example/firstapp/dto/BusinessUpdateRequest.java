package com.example.firstapp.dto;
import com.example.firstapp.enums.CompanySize;
import com.example.firstapp.enums.IndustryType;

public record BusinessUpdateRequest(
    String companyName,
    CompanySize companySize,
    String website,
    String description,
    AddressRequest headquarters,
    String contactPhone,
    IndustryType industryType,
    String logoUrl
    // registrationNumber, contactEmail, verificationStatus deliberately excluded —
    // changing a legal registration number or verified contact email should go through
    // a re-verification flow, not a plain profile-edit endpoint
) {}
