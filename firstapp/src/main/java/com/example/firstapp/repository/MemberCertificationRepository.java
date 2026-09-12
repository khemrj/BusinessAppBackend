package com.example.firstapp.repository;

import com.example.firstapp.entity.MemberCertification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberCertificationRepository
        extends JpaRepository<MemberCertification, Long> {

    List<MemberCertification>
    findByMemberIdOrderByIssueDateDesc(Long memberId);
}