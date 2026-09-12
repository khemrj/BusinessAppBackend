package com.example.firstapp.repository;

import com.example.firstapp.entity.Member;
import com.example.firstapp.entity.MemberExperience;
import com.example.firstapp.entity.MemberEducation;
import com.example.firstapp.entity.MemberCertification;
import com.example.firstapp.enums.ProfileVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.firstapp.enums.FinanceSpecialization;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByProfileSlug(String profileSlug);

    /**
     * SINGLE PROFILE VIEW — Step 1 of the multi-query strategy.
     *
     * EntityGraph eagerly loads:
     * → user (singular — always safe to join)
     * → location is @Embedded — comes for free, no join needed
     * → skills (Set-backed @ElementCollection — NOT a bag, safe
     *   to combine with the singular "user" join above)
     *
     * Deliberately does NOT include experiences/educations/
     * certifications here — those are 3 separate bags, and
     * combining even TWO of them throws MultipleBagFetchException.
     */
    @EntityGraph(attributePaths = {"user", "skills"})
    Optional<Member> findByProfileSlug(String profileSlug);

    @EntityGraph(attributePaths = {"user", "skills"})
    Optional<Member> findWithCoreDataById(Long id);

    /**
     * SINGLE PROFILE VIEW — Steps 2, 3, 4.
     *
     * Each is its own tiny, indexed query (member_id is FK-indexed
     * on all three child tables via mappedBy). Running all three
     * for ONE profile = 3 fast queries, NOT the N+1 problem —
     * N+1 only occurs when this pattern repeats per-row across
     * a LIST of many members.
     *
     * Ordering matters for a profile page (most recent role first) —
     * add ORDER BY explicitly since a bag has no natural order.
     */
    @Query("SELECT e FROM MemberExperience e " +
           "WHERE e.member.id = :memberId " +
           "ORDER BY e.startDate DESC")
    List<MemberExperience> findExperiencesByMemberId(
        @Param("memberId") Long memberId
    );

    @Query("SELECT ed FROM MemberEducation ed " +
           "WHERE ed.member.id = :memberId " +
           "ORDER BY ed.endDate DESC")
    List<MemberEducation> findEducationsByMemberId(
        @Param("memberId") Long memberId
    );

    @Query("SELECT c FROM MemberCertification c " +
           "WHERE c.member.id = :memberId " +
           "ORDER BY c.issueDate DESC")
    List<MemberCertification> findCertificationsByMemberId(
        @Param("memberId") Long memberId
    );

    /**
     * SEARCH / LIST VIEW — where @BatchSize actually earns its keep.
     *
     * Loading a PAGE of members (e.g. connections list, search
     * results). Without @BatchSize on Member's collections, if the
     * UI later touches member.getSkills() for each of these 20
     * results, that's 20 separate SELECTs = real N+1.
     *
     * With @BatchSize(size = 25) already on the entity, Hibernate
     * instead issues ONE extra query:
     *   SELECT * FROM member_skills WHERE member_id IN (?, ?, ?...)
     * for the whole batch — this is the actual payoff of the
     * annotation you already added.
     */
    Page<Member> findByProfileVisibility(
        ProfileVisibility visibility,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"user"})
    Page<Member> findBySpecialization(
        FinanceSpecialization specialization,
        Pageable pageable
    );
    @Query("""
            
        SELECT m.id FROM Member m WHERE m.user.id = :userId
    """)
    Optional<Long> findMemberIdByUserId(@Param("userId") Long userId);  
            
}