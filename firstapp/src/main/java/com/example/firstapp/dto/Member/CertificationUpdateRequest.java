package com.example.firstapp.dto.Member;


import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CertificationUpdateRequest(

        @Size(
                max = 200,
                message = "Certification name must not exceed 200 characters"
        )
        String name,

        @Size(
                max = 200,
                message = "Issuing organization must not exceed 200 characters"
        )
        String issuingOrganization,

        LocalDate issueDate,

        LocalDate expirationDate,

        @Size(
                max = 150,
                message = "Credential ID must not exceed 150 characters"
        )
        String credentialId,

        @Size(
                max = 500,
                message = "Credential URL must not exceed 500 characters"
        )
        String credentialUrl

) {
}
