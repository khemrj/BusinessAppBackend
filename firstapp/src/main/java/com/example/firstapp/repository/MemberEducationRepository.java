package com.example.firstapp.repository;

import com.example.firstapp.entity.MemberEducation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberEducationRepository
        extends JpaRepository<MemberEducation, Long> {

    List<MemberEducation>
    findByMemberIdOrderByStartDateDesc(Long memberId);

    /** * Find an education record only when it belongs * to the specified member. */
     Optional<MemberEducation> findByIdAndMemberUserId( Long educationId, Long userId );
    
    
}