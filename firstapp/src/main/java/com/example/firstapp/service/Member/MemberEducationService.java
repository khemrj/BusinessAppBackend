package com.example.firstapp.service.Member;

import com.example.firstapp.dto.Member.EducationRequest;
import com.example.firstapp.dto.Member.EducationResponse;
import com.example.firstapp.dto.Member.EducationUpdateRequest;
import com.example.firstapp.entity.Member;
import com.example.firstapp.entity.MemberEducation;
import com.example.firstapp.entity.User;
import com.example.firstapp.exception.ResourceNotFoundException;
import com.example.firstapp.exception.UnauthorizedException;
import com.example.firstapp.repository.MemberEducationRepository;
import com.example.firstapp.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberEducationService {

    private final MemberRepository memberRepository;

    private final MemberEducationRepository educationRepository;

    /**
     * getReferenceById avoids a full SELECT on Member just to
     * attach a child row — no need to touch Member's 3 bags
     * (experiences/educations/certifications) for this write.
     */
    @Transactional
    public EducationResponse add(Long memberId, EducationRequest request) {

        Member member = memberRepository.getReferenceById(memberId);

        MemberEducation education = MemberEducation.builder()
                .member(member)
                .institutionName(request.institution())
                .degree(request.degree())
                .fieldOfStudy(request.fieldOfStudy())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();

        return EducationResponse.fromEntity(educationRepository.save(education));
    }

    @Transactional
    public EducationResponse updateMyEducation(
             @AuthenticationPrincipal User user,
            Long educationId,
            EducationUpdateRequest request
    ) {

        MemberEducation education = educationRepository.findById(educationId)
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

        // Dirty checking flushes changes when the transaction commits.
        return EducationResponse.fromEntity(education);
    }

    @Transactional
    public void delete(Long memberId, Long educationId) {

        MemberEducation education = educationRepository.findById(educationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Education",
                        "id",
                        educationId
                ));

        assertOwnership(education.getMember().getId(), memberId);

        educationRepository.delete(education);
    }
// fetching education detail for current user
   @Transactional(readOnly = true)
public List<EducationResponse> getEducationByMemberId(Long memberId) {

    return educationRepository
            .findByMemberIdOrderByStartDateDesc(memberId)
            .stream()
            .map(EducationResponse::fromEntity)
            .toList();
}

    private void assertOwnership(Long ownerId, Long requesterId) {

        if (!ownerId.equals(requesterId)) {
            throw new UnauthorizedException(
                    "You do not have permission to modify this record"
            );
        }
    }
}