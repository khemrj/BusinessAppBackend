package com.example.firstapp.service.Member;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.firstapp.dto.Member.ExperienceRequest;
import com.example.firstapp.dto.Member.ExperienceResponse;
import com.example.firstapp.entity.Member;
import com.example.firstapp.entity.MemberExperience;
import com.example.firstapp.repository.MemberExperienceRepository;
import com.example.firstapp.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberExperienceService {

    private final MemberRepository memberRepository;
    private final MemberExperienceRepository experienceRepository;

    @Transactional
    public ExperienceResponse add(Long memberId, ExperienceRequest request) {
        Member member = memberRepository.getReferenceById(memberId);
        // getReferenceById = no SELECT needed, just a proxy for the FK —
        // avoids loading the full Member (with its 3 bags) just to attach a child

        MemberExperience experience = MemberExperience.builder()
            .member(member)
            .companyName(request.companyName())
            .jobTitle(request.title())
            .startDate(request.startDate())
            .endDate(request.endDate())
            .description(request.description())
            .build();

        return ExperienceResponse.fromEntity(experienceRepository.save(experience));
    }
}
