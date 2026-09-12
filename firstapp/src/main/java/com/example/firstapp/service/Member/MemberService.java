package com.example.firstapp.service.Member;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.firstapp.dto.Member.MemberCreateRequest;
import com.example.firstapp.dto.Member.MemberProfileResponse;
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
}