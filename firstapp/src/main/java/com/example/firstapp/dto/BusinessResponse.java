package com.example.firstapp.dto;

import com.example.firstapp.entity.Address;
import com.example.firstapp.entity.Business;
import com.example.firstapp.enums.BusinessStatus;
import com.example.firstapp.enums.CompanySize;
import com.example.firstapp.enums.IndustryType;
import com.example.firstapp.enums.VerificationStatus;

import java.time.LocalDateTime;

public record BusinessResponse(
    Long id,
    String companyName,
    String registrationNumber,
    IndustryType industryType,
    CompanySize companySize,
    String website,
    String description,
    String logoUrl,
    Address headquarters,
    String contactEmail,
    String contactPhone,
    VerificationStatus verificationStatus,
    LocalDateTime verifiedAt,
    BusinessStatus status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static BusinessResponse fromEntity(Business b) {
        return new BusinessResponse(
            b.getId(),
            b.getCompanyName(),
            b.getRegistrationNumber(),
            b.getIndustryType(),
            b.getCompanySize(),
            b.getWebsite(),
            b.getDescription(),
            b.getLogoUrl(),
            b.getHeadquarters(),     // @Embeddable value object, no entity refs inside — safe to expose
            b.getContactEmail(),
            b.getContactPhone(),
            b.getVerificationStatus(),
            b.getVerifiedAt(),
            b.getStatus(),
            b.getCreatedAt(),
            b.getUpdatedAt()
        );
    }
}