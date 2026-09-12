package com.example.firstapp.dto.Member;

import com.example.firstapp.entity.MemberCertification;

import java.time.LocalDate;

public record CertificationResponse(

        Long id,

        String name,

        String issuingOrganization,

        LocalDate issueDate,

        LocalDate expirationDate,

        String credentialId,

        String credentialUrl

) {

    public static CertificationResponse fromEntity(
            MemberCertification certification
    ) {

        if (certification == null) {
            return null;
        }

        return new CertificationResponse(
                certification.getId(),
                certification.getTitle(),
                certification.getIssuingOrganization(),
                certification.getIssueDate(),
                certification.getExpirationDate(),
                certification.getCredentialId(),
                certification.getCredentialUrl()
        );
    }
}
