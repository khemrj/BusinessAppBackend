package com.example.firstapp.service.Member;

import org.springframework.stereotype.Service;

import com.example.firstapp.exception.ResourceNotFoundException;
import com.example.firstapp.repository.MemberRepository;

import com.example.firstapp.dto.Member.MemberProfileResponse;
import com.example.firstapp.entity.Member;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberProfileService {

    private final MemberRepository memberRepository;

    /**
     * Single @Transactional boundary → all 4 queries share one
     * DB connection/session, execute back-to-back, no lazy-init
     * exceptions on return since we map to DTOs before the
     * transaction closes.
     */
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
}