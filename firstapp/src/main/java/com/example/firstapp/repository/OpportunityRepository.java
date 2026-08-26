package com.example.firstapp.repository;

import com.example.firstapp.entity.Opportunity;
import com.example.firstapp.enums.FinanceSpecialization;
import com.example.firstapp.enums.OpportunityStatus;
import com.example.firstapp.enums.OpportunityType;
import com.example.firstapp.enums.WorkMode;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OpportunityRepository
        extends JpaRepository<Opportunity, Long> {

    /**
     * Get all opportunities belonging to a business.
     * Kept for any code path that doesn't need collections loaded.
     * "Business_Id" (underscore) tells Spring Data explicitly to traverse
     * the `business` relation and filter on its `id`, rather than guessing.
     */
    List<Opportunity> findByBusiness_Id(Long businessId);

    /**
     * Get published opportunities.
     * Kept for any code path that doesn't need collections loaded.
     */
    List<Opportunity> findByStatus(OpportunityStatus status);

    /**
     * Get published opportunities for a specific specialization.
     */
    List<Opportunity> findByStatusAndSpecialization(
            OpportunityStatus status,
            FinanceSpecialization specialization
    );

    /**
     * Filter by type.
     */
    List<Opportunity> findByStatusAndType(
            OpportunityStatus status,
            OpportunityType type
    );

    /**
     * Filter by work mode.
     */
    List<Opportunity> findByStatusAndWorkMode(
            OpportunityStatus status,
            WorkMode workMode
    );

    /**
     * Useful for authorization checks.
     *
     * Makes sure an opportunity belongs to the
     * authenticated business/user.
     */
    boolean existsByIdAndBusiness_Id(
            Long id,
            Long businessId
    );

    // ------------------------------------------------------------------
    // Fetch-join variants: JOIN FETCH responsibilities in one query.
    // requiredSkills / qualifications ride along via @BatchSize on the
    // entity (see Opportunity.java) — Hibernate can't JOIN FETCH more
    // than one List collection at a time (MultipleBagFetchException).
    // ------------------------------------------------------------------

    /**
     * Single opportunity, with responsibilities fetch-joined.
     * Used for GET /api/v1/opportunities/{id}.
     */
    @Query("""
        SELECT o FROM Opportunity o
        LEFT JOIN FETCH o.responsibilities
        WHERE o.id = :id
        """)
    Optional<Opportunity> findByIdWithResponsibilities(
            @Param("id") Long id
    );

    /**
     * Published opportunities, with responsibilities fetch-joined.
     * Used for GET /api/v1/opportunities (public listing).
     * DISTINCT avoids duplicate Opportunity rows from the join.
     */
    @Query("""
        SELECT DISTINCT o FROM Opportunity o
        LEFT JOIN FETCH o.responsibilities
        WHERE o.status = :status
        """)
    List<Opportunity> findByStatusWithResponsibilities(
            @Param("status") OpportunityStatus status
    );

    /**
     * A business's own opportunities, with responsibilities fetch-joined.
     * Used for GET /api/v1/opportunities/mine (or similar).
     * Note: o.business.id — traverses the @ManyToOne relation, since
     * Opportunity no longer has a flat businessId scalar field.
     */
    @Query("""
        SELECT DISTINCT o FROM Opportunity o
        LEFT JOIN FETCH o.responsibilities
        WHERE o.business.id = :businessId
        """)
    List<Opportunity> findByBusinessIdWithResponsibilities(
            @Param("businessId") Long businessId
    );
}