package com.example.firstapp.repository;

import com.example.firstapp.entity.MemberExperience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberExperienceRepository
        extends JpaRepository<MemberExperience, Long> {

    List<MemberExperience>
    findByMemberIdOrderByStartDateDesc(Long memberId);
       Optional<MemberExperience> findByIdAndMemberId(//therefore yoou no longer need assertOwnership
            Long id,
            Long memberId
    );
}