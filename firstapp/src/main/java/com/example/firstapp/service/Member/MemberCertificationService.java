
package com.example.firstapp.service.Member;

import com.example.firstapp.dto.Member.CertificationRequest;
import com.example.firstapp.dto.Member.CertificationResponse;
import com.example.firstapp.entity.Member;
import com.example.firstapp.entity.MemberCertification;
import com.example.firstapp.exception.ResourceNotFoundException;
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
     * Add a certification to the authenticated user's profile.
     */
    @Transactional
    public CertificationResponse add(
            Long userId,
            CertificationRequest request
    ) {

        Long memberId = memberRepository
                .findMemberIdByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member",
                        "userId",
                        userId
                ));

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

        certificationRepository.save(certification);

        return CertificationResponse.fromEntity(certification);
    }


    /**
     * Update a certification belonging to the authenticated user.
     */
    @Transactional
    public CertificationResponse update(
            Long userId,
            Long certificationId,
            CertificationRequest request
    ) {

        Long memberId = memberRepository
                .findMemberIdByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member",
                        "userId",
                        userId
                ));

        MemberCertification certification =
                certificationRepository
                        .findByIdAndMemberId(
                                certificationId,
                                memberId
                        )
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Certification",
                                "id",
                                certificationId
                        ));

        certification.setTitle(request.name());

        certification.setIssuingOrganization(
                request.issuingOrganization()
        );

        certification.setIssueDate(
                request.issuedDate()
        );

        certification.setExpirationDate(
                request.expiryDate()
        );

        certification.setCredentialId(
                request.credentialUrl()
        );

        certification.setCredentialUrl(
                request.credentialUrl()
        );

        // No save() is required here.
        // Hibernate dirty checking updates the managed entity
        // when the transaction commits.

        return CertificationResponse.fromEntity(certification);
    }


    /**
     * Delete a certification belonging to the authenticated user.
     */
    @Transactional
    public void delete(
            Long userId,
            Long certificationId
    ) {

        Long memberId = memberRepository
                .findMemberIdByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member",
                        "userId",
                        userId
                ));

        MemberCertification certification =
                certificationRepository
                        .findByIdAndMemberId(
                                certificationId,
                                memberId
                        )
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Certification",
                                "id",
                                certificationId
                        ));

        certificationRepository.delete(certification);
    }


    /**
     * Get all certifications belonging to the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<CertificationResponse> getMyCertifications(
            Long userId
    ) {

        Long memberId = memberRepository
                .findMemberIdByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member",
                        "userId",
                        userId
                ));

        return certificationRepository
                .findByMemberIdOrderByIssueDateDesc(memberId)
                .stream()
                .map(CertificationResponse::fromEntity)
                .toList();
    }
}

