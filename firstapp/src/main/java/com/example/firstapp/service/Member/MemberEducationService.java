
package com.example.firstapp.service.Member;

import com.example.firstapp.dto.Member.EducationRequest;
import com.example.firstapp.dto.Member.EducationResponse;
import com.example.firstapp.dto.Member.EducationUpdateRequest;
import com.example.firstapp.entity.Member;
import com.example.firstapp.entity.MemberEducation;
import com.example.firstapp.exception.ResourceNotFoundException;
import com.example.firstapp.repository.MemberEducationRepository;
import com.example.firstapp.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberEducationService {

    private final MemberRepository memberRepository;
    private final MemberEducationRepository educationRepository;


    /**
     * Add education to the authenticated user's profile.
     */
    @Transactional
    public EducationResponse addEducation(
            Long userId,
            EducationRequest request
    ) {

        Long memberId = memberRepository
                .findMemberIdByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member",
                        "userId",
                        userId
                ));

        // No need to load the complete Member entity.
        // We only need it as the FK relationship.
        Member member = memberRepository.getReferenceById(memberId);

        MemberEducation education =
                MemberEducation.builder()
                        .member(member)
                        .institutionName(request.institution())
                        .degree(request.degree())
                        .fieldOfStudy(request.fieldOfStudy())
                        .startDate(request.startDate())
                        .endDate(request.endDate())
                        .build();

        educationRepository.save(education);

        return EducationResponse.fromEntity(education);
    }


    /**
     * Update an education record belonging to the authenticated user.
     */
    @Transactional
    public EducationResponse updateMyEducation(
            Long userId,
            Long educationId,
            EducationUpdateRequest request
    ) {

        MemberEducation education =
                educationRepository
                        .findByIdAndMemberUserId(
                                educationId,
                                userId
                        )
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Education",
                                "id",
                                educationId
                        ));

        education.setInstitutionName(request.institution());
        education.setDegree(request.degree());
        education.setFieldOfStudy(request.fieldOfStudy());
        education.setStartDate(request.startDate());
        education.setEndDate(request.endDate());

        /*
         * No save() is required.
         *
         * The entity is managed by Hibernate because it was loaded
         * inside the @Transactional method.
         *
         * Hibernate dirty checking detects the changes and generates
         * the UPDATE SQL when the transaction commits.
         */

        return EducationResponse.fromEntity(education);
    }


    /**
     * Delete an education record belonging to the authenticated user.
     */
    @Transactional
    public void deleteMyEducation(
            Long userId,
            Long educationId
    ) {

        MemberEducation education =
                educationRepository
                        .findByIdAndMemberUserId(
                                educationId,
                                userId
                        )
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Education",
                                "id",
                                educationId
                        ));

        educationRepository.delete(education);
    }


    /**
     * Get all education records belonging to the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<EducationResponse> getMyEducations(
            Long userId
    ) {

        Long memberId = memberRepository
                .findMemberIdByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member",
                        "userId",
                        userId
                ));

        return educationRepository
                .findByMemberIdOrderByStartDateDesc(memberId)
                .stream()
                .map(EducationResponse::fromEntity)
                .toList();
    }
}

