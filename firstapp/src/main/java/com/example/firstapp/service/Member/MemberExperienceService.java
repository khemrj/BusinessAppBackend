
package com.example.firstapp.service.Member;

import com.example.firstapp.dto.Member.ExperienceRequest;
import com.example.firstapp.dto.Member.ExperienceResponse;
import com.example.firstapp.entity.Member;
import com.example.firstapp.entity.MemberExperience;
import com.example.firstapp.exception.ResourceNotFoundException;
import com.example.firstapp.repository.MemberExperienceRepository;
import com.example.firstapp.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberExperienceService {

    private final MemberRepository memberRepository;
    private final MemberExperienceRepository experienceRepository;


    /**
     * Add an experience to the authenticated user's profile.
     */
    @Transactional
    public ExperienceResponse addExperience(
            Long userId,
            ExperienceRequest request
    ) {

        Long memberId = memberRepository
                .findMemberIdByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member",
                        "userId",
                        userId
                ));

        Member member = memberRepository.getReferenceById(memberId);

        MemberExperience experience =
                MemberExperience.builder()
                        .member(member)
                        .companyName(request.companyName())
                        .jobTitle(request.title())
                        .startDate(request.startDate())
                        .endDate(request.endDate())
                        .description(request.description())
                        .build();

        experienceRepository.save(experience);

        return ExperienceResponse.fromEntity(experience);
    }


    /**
     * Update an experience belonging to the authenticated user.
     */
    @Transactional
    public ExperienceResponse updateMyExperience(
            Long userId,
            Long experienceId,
            ExperienceRequest request
    ) {

        Long memberId = memberRepository
                .findMemberIdByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member",
                        "userId",
                        userId
                ));

        MemberExperience experience =
                experienceRepository
                        .findByIdAndMemberId(
                                experienceId,
                                memberId
                        )
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Experience",
                                "id",
                                experienceId
                        ));

        experience.setCompanyName(request.companyName());
        experience.setJobTitle(request.title());
        experience.setStartDate(request.startDate());
        experience.setEndDate(request.endDate());
        experience.setDescription(request.description());

        // No save() required.
        // The entity is managed and Hibernate dirty checking
        // will generate the UPDATE when the transaction commits.

        return ExperienceResponse.fromEntity(experience);
    }


    /**
     * Delete an experience belonging to the authenticated user.
     */
    @Transactional
    public void deleteMyExperience(
            Long userId,
            Long experienceId
    ) {

        Long memberId = memberRepository
                .findMemberIdByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member",
                        "userId",
                        userId
                ));

        MemberExperience experience =
                experienceRepository
                        .findByIdAndMemberId(
                                experienceId,
                                memberId
                        )
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Experience",
                                "id",
                                experienceId
                        ));

        experienceRepository.delete(experience);
    }


    /**
     * Get all experiences belonging to the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<ExperienceResponse> getMyExperiences(
            Long userId
    ) {

        Long memberId = memberRepository
                .findMemberIdByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member",
                        "userId",
                        userId
                ));

        return experienceRepository
                .findByMemberIdOrderByStartDateDesc(memberId)
                .stream()
                .map(ExperienceResponse::fromEntity)
                .toList();
    }
}
