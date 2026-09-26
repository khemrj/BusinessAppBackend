package com.example.firstapp.service.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.firstapp.dto.UpdateMemberRequest;
import com.example.firstapp.dto.Member.MemberCreateRequest;
import com.example.firstapp.dto.Member.MemberProfileResponse;
import com.example.firstapp.entity.Address;
import com.example.firstapp.entity.Member;
import com.example.firstapp.entity.User;
import com.example.firstapp.enums.ProfileVisibility;
import com.example.firstapp.exception.ResourceNotFoundException;
import com.example.firstapp.repository.MemberRepository;
import com.example.firstapp.service.ProfileSlugService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final ProfileSlugService slugService;

    @Transactional
    public MemberProfileResponse createMember(User user, MemberCreateRequest request) {

        String slug = slugService.generate(
            request.firstName(), request.lastName()
        );

        Member member = Member.builder()
            .user(user)
            .firstName(request.firstName())
            .lastName(request.lastName())
            .profileSlug(slug)
            .headline(request.headline())
            .bio(request.bio())
            .specialization(request.specialization())
            .contactEmail(request.contactEmail())
            .phone(request.phone())
            .yearsOfExperience(request.yearsOfExperience())
            .githubUrl(request.githubUrl())
            .linkedinUrl(request.linkedinUrl())
            .websiteUrl(request.websiteUrl())
            .location(request.location() != null
                ? request.location().toEntity() : null)
            // Never trust client-sent visibility blindly for sensitive
            // defaults — but for a self-service profile, honoring the
            // request with a safe fallback is fine
            .profileVisibility(request.profileVisibility() != null
                ? request.profileVisibility()
                : ProfileVisibility.PUBLIC)
            .openToWork(request.openToWork() != null
                ? request.openToWork() : false)
            .build();

        return MemberProfileResponse.fromEntity(memberRepository.save(member));
    }

   @Transactional(readOnly = true)
    public Long getMemberIdByUserId(Long userId) {

    return memberRepository.findMemberIdByUserId(userId)
            .orElseThrow(() ->
                    new ResourceNotFoundException(
                            "Member",
                            "userId",
                            userId
                    )
            );
} 

@Transactional(readOnly = true)
    public MemberProfileResponse getProfileBySlug(String slug) {

        Member member = memberRepository.findByProfileSlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Member", "profileSlug", slug
            ));

        return MemberProfileResponse.fromEntity(  // prev it was member, experiences, educations, certifications
            member
        );
    }
    // self made update memver service function 
    /**
     * Update the profile belonging to the authenticated user.
     */
    @Transactional
    public MemberProfileResponse updateMyProfile(
            Long userId,
            UpdateMemberRequest request
    ) {
        Long memberId = memberRepository
                .findMemberIdByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member",
                        "userId",
                        userId
                ));

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Member",
                        "memberId",
                        memberId
                ));

        member.setFirstName(request.firstName());
        member.setLastName(request.lastName());
        member.setPhone(request.phone());
        member.setHeadline(request.headline());
        member.setBio(request.bio());
        member.setContactEmail(request.contactEmail());
        member.setWebsiteUrl(request.websiteUrl());
        member.setLinkedinUrl(request.linkedinUrl());
        member.setGithubUrl(request.githubUrl());

        // Update embedded or associated address if location is provided
        if (request.location() != null) {
            if (member.getLocation() == null) {
                member.setLocation(new Address());
            }
            member.getLocation().setStreet(request.location().street());
            member.getLocation().setCity(request.location().city());
            member.getLocation().setState(request.location().state());
            member.getLocation().setCountry(request.location().country());
            member.getLocation().setPostalCode(request.location().postalCode());
        }

        // No save() required.
        // The entity is managed and Hibernate dirty checking
        // will generate the UPDATE when the transaction commits.

        return MemberProfileResponse.fromEntity(member);
    }

}