package com.example.firstapp.service.Member;


import com.example.firstapp.dto.Member.CertificationRequest;
import com.example.firstapp.dto.Member.CertificationResponse;
import com.example.firstapp.entity.Member;
import com.example.firstapp.entity.MemberCertification;
import com.example.firstapp.exception.ResourceNotFoundException;
import com.example.firstapp.exception.UnauthorizedException;
import com.example.firstapp.repository.MemberCertificationRepository;
import com.example.firstapp.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberCertificationService {

    private final MemberRepository memberRepository;
    private final MemberCertificationRepository certificationRepository;


    /**
     * Add a certification to a member profile.
     *
     * getReferenceById() avoids loading the complete Member entity.
     * We only need a reference to establish the relationship.
     */
    @Transactional
    public CertificationResponse add(
            Long memberId,
            CertificationRequest request
    ) {

        Member member = memberRepository.getReferenceById(memberId);

        MemberCertification certification =
                MemberCertification.builder()
                        .member(member)
                        .title(request.name())
                        .issuingOrganization(request.issuingOrganization())
                        .issueDate(request.issuedDate())
                        .expirationDate(request.expiryDate())
                        .credentialId(request.credentialUrl())
                        .credentialUrl(request.credentialUrl())
                        .build();

        return CertificationResponse.fromEntity(certificationRepository.save(certification));
    }


    /**
     * Update an existing certification.
     */
    @Transactional
    public CertificationResponse update(
            Long memberId,
            Long certificationId,
            CertificationRequest request
    ) {

        MemberCertification certification =
                certificationRepository.findById(certificationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Certification",
                                        "id",
                                        certificationId
                                )
                        );

        // Prevent member A from modifying member B's certification.
        assertOwnership(
                certification.getMember().getId(),
                memberId
        );

        certification.setTitle(request.name());
        certification.setIssuingOrganization(
                request.issuingOrganization()
        );
        certification.setIssueDate(request.issuedDate());
        certification.setExpirationDate(
                request.expiryDate()
        );
        certification.setCredentialId(
                request.credentialUrl()
        );
        certification.setCredentialUrl(
                request.credentialUrl()
        );

        // Hibernate dirty checking updates the entity.
        return CertificationResponse.fromEntity(certificationRepository.save(certification));
    }


    /**
     * Delete a certification.
     */
    @Transactional
    public void delete(
            Long memberId,
            Long certificationId
    ) {

        MemberCertification certification =
                certificationRepository.findById(certificationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Certification",
                                        "id",
                                        certificationId
                                )
                        );

        assertOwnership(
                certification.getMember().getId(),
                memberId
        );

        certificationRepository.delete(certification);
    }


    /**
     * Get all certifications belonging to a member.
     */
    @Transactional(readOnly = true)
    public List<MemberCertification> getByMemberId(
            Long memberId
    ) {

        return certificationRepository
                .findByMemberIdOrderByIssueDateDesc(memberId);
    }


    /**
     * Verify that the resource belongs to the requested member.
     */
    private void assertOwnership(
            Long ownerId,
            Long requesterId
    ) {

        if (!ownerId.equals(requesterId)) {
            throw new UnauthorizedException(
                    "You do not have permission to modify this record"
            );
        }
    }
}