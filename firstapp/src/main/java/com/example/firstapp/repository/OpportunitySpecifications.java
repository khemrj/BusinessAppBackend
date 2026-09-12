package com.example.firstapp.repository;

import org.springframework.data.jpa.domain.Specification;

import com.example.firstapp.entity.Opportunity;
import com.example.firstapp.enums.ExperienceLevel;
import com.example.firstapp.enums.FinanceSpecialization;
import com.example.firstapp.enums.OpportunityStatus;
import com.example.firstapp.enums.OpportunityType;
import com.example.firstapp.enums.WorkMode;

public final class OpportunitySpecifications {
    private OpportunitySpecifications() {}
    
    public static Specification<Opportunity> isPublished() {
        return (root, query, cb) -> cb.equal(root.get("status"), OpportunityStatus.PUBLISHED);
    }

    public static Specification<Opportunity> hasSpecialization(FinanceSpecialization value) {
        // Returning null here is intentional and safe: Spring Data JPA's
        // Specification.and() treats a null predicate as "no restriction"
        // when composing, so an absent filter just doesn't narrow the query.
        return (root, query, cb) -> value == null ? null : cb.equal(root.get("specialization"), value);
    }

    public static Specification<Opportunity> hasWorkMode(WorkMode value) {
        return (root, query, cb) -> value == null ? null : cb.equal(root.get("workMode"), value);
    }

    public static Specification<Opportunity> hasExperienceLevel(ExperienceLevel value) {
        return (root, query, cb) -> value == null ? null : cb.equal(root.get("experienceLevel"), value);
    }

    public static Specification<Opportunity> hasType(OpportunityType value) {
        return (root, query, cb) -> value == null ? null : cb.equal(root.get("type"), value);
    }
}