package com.example.firstapp.repository;

import com.example.firstapp.entity.MemberEducation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberEducationRepository
        extends JpaRepository<MemberEducation, Long> {

    List<MemberEducation>
    findByMemberIdOrderByStartDateDesc(Long memberId);
}