package com.example.firstapp.entity;

import com.example.firstapp.enums.ExperienceLevel;
import com.example.firstapp.enums.FinanceSpecialization;
import com.example.firstapp.enums.OpportunityStatus;
import com.example.firstapp.enums.OpportunityType;
import com.example.firstapp.enums.WorkMode;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "opportunities",
    indexes = {
        @Index(
            name = "idx_opportunity_business",
            columnList = "business_id"
        ),
        @Index(
            name = "idx_opportunity_status",
            columnList = "status"
        ),
        @Index(
            name = "idx_opportunity_specialization",
            columnList = "specialization"
        ),
        @Index(
            name = "idx_opportunity_type",
            columnList = "type"
        ),
        @Index(
            name = "idx_opportunity_work_mode",
            columnList = "work_mode"
        )
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Business/user that owns this opportunity.
     *
     * IMPORTANT:
     * This should NOT come from the POST request.
     * It should be taken from the authenticated JWT user.
     */
   @ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "business_id", nullable = false) //businessID 
    private Business business;

   @Column(name = "business_name", nullable = false, length = 150)
    private String businessName; 

    @Column(
        name = "title",
        nullable = false,
        length = 200
    )
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "specialization",
        nullable = false,
        length = 50
    )
    private FinanceSpecialization specialization;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "type",
        nullable = false,
        length = 30
    )
    private OpportunityType type;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "experience_level",
        nullable = false,
        length = 30
    )
    private ExperienceLevel experienceLevel;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "work_mode",
        nullable = false,
        length = 20
    )
    private WorkMode workMode;

    @Column(
        name = "location",
        nullable = false,
        length = 200
    )
    private String location;

    @Column(
        name = "description",
        nullable = false,
        columnDefinition = "TEXT"
    )
    private String description;

    /**
     * Responsibilities can contain multiple strings.
     *
     * This is the collection we JOIN FETCH directly in the repository
     * (see OpportunityRepository) for the hot-path list/detail queries.
     * @BatchSize here is a safety net for any code path that reaches
     * this collection WITHOUT going through a fetch-join query.
     */
    @ElementCollection
    @CollectionTable(
        name = "opportunity_responsibilities",
        joinColumns = @JoinColumn(name = "opportunity_id")
    )
    @Column(
        name = "responsibility",
        nullable = false,
        columnDefinition = "TEXT"
    )
    @BatchSize(size = 25)
    @Builder.Default
    private List<String> responsibilities = new ArrayList<>();

    /**
     * Required skills.
     *
     * Hibernate can only JOIN FETCH one List/bag collection per query,
     * so this one relies on @BatchSize instead of a fetch join.
     */
    @ElementCollection
    @CollectionTable(
        name = "opportunity_required_skills",
        joinColumns = @JoinColumn(name = "opportunity_id")
    )
    @Column(
        name = "skill",
        nullable = false,
        length = 150
    )
    @BatchSize(size = 25)
    @Builder.Default
    private List<String> requiredSkills = new ArrayList<>();

    /**
     * Qualifications.
     *
     * Same as requiredSkills — batched, not fetch-joined.
     */
    @ElementCollection
    @CollectionTable(
        name = "opportunity_qualifications",
        joinColumns = @JoinColumn(name = "opportunity_id")
    )
    @Column(
        name = "qualification",
        nullable = false,
        columnDefinition = "TEXT"
    )
    @BatchSize(size = 25)
    @Builder.Default
    private List<String> qualifications = new ArrayList<>();

    /**
     * Compensation is stored in the same opportunities table.
     */
    @jakarta.persistence.Embedded
    private CompensationRange compensation;

    @Column(
        name = "openings",
        nullable = false
    )
    private Integer openings;

    @Enumerated(EnumType.STRING)
    @Column(
        name = "status",
        nullable = false,
        length = 20
    )
    @Builder.Default
    private OpportunityStatus status = OpportunityStatus.DRAFT;

    @Column(
        name = "posted_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime postedAt;

    @Column(
        name = "application_deadline"
    )
    private LocalDateTime applicationDeadline;

    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (postedAt == null) {
            postedAt = now;
        }

        if (status == null) {
            status = OpportunityStatus.DRAFT;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}