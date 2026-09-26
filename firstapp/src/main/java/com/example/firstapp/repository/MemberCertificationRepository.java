package com.example.firstapp.repository;

import com.example.firstapp.entity.MemberCertification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberCertificationRepository
        extends JpaRepository<MemberCertification, Long> {
    

     Optional<MemberCertification> findByIdAndMemberId(
            Long id,
            Long memberId
    );        
    List<MemberCertification>
    findByMemberIdOrderByIssueDateDesc(Long memberId);
}