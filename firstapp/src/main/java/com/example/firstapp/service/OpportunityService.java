package com.example.firstapp.service;

import com.example.firstapp.dto.OpportunityCreateRequest;
import com.example.firstapp.dto.OpportunityResponse;
import com.example.firstapp.dto.OpportunityUpdateRequest;
import com.example.firstapp.dto.Mapper.OpportunityMapper;
import com.example.firstapp.entity.Business;
import com.example.firstapp.entity.CompensationRange;
import com.example.firstapp.entity.Opportunity;
import com.example.firstapp.entity.User;
import com.example.firstapp.enums.ExperienceLevel;
import com.example.firstapp.enums.FinanceSpecialization;
import com.example.firstapp.enums.OpportunityStatus;
import com.example.firstapp.enums.OpportunityType;
import com.example.firstapp.enums.WorkMode;
import com.example.firstapp.repository.OpportunityRepository;
import com.example.firstapp.repository.OpportunitySpecifications;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final BusinessService businessService;
    private final OpportunityMapper opportunityMapper;
      
    
    /**
     * Create a new opportunity.
     *
     * Business identity comes from authenticated User,
     * NOT from the Flutter request.
     */
    @Transactional
public OpportunityResponse createOpportunity(OpportunityCreateRequest request, User currentUser) {

    log.info("Creating opportunity for user: {}", currentUser.getId());

    Business business = businessService.getByOwnerUserId(currentUser.getId());   // ← real row, not User.id

    CompensationRange compensation = CompensationRange.builder()
            .min(request.getCompensation().getMin())
            .max(request.getCompensation().getMax())
            .currency(request.getCompensation().getCurrency())
            .disclosed(request.getCompensation().isDisclosed())
            .build();

    Opportunity opportunity = Opportunity.builder()
            .business(business)                       // was: Business.builder().id(currentUser.getId()).build()
            .businessName(business.getCompanyName())   // was: currentUser.getUsername()
            .title(request.getTitle())
            .specialization(request.getSpecialization())
            .type(request.getType())
            .experienceLevel(request.getExperienceLevel())
            .workMode(request.getWorkMode())
            .location(request.getLocation())
            .description(request.getDescription())
            .responsibilities(request.getResponsibilities())
            .requiredSkills(request.getRequiredSkills())
            .qualifications(request.getQualifications())
            .compensation(compensation)
            .openings(request.getOpenings())
            .status(OpportunityStatus.PUBLISHED)
            .applicationDeadline(request.getApplicationDeadline())
            .build();

    Opportunity saved = opportunityRepository.save(opportunity);
    log.info("Opportunity created successfully: {}", saved.getId());
    return OpportunityResponse.fromEntity(saved);
}

    /**
     * Get a single opportunity by ID.
     *
     * Uses the fetch-join query so responsibilities come back in the
     * same SELECT — no separate lazy-load round trip.
     */
    @Transactional(readOnly = true)
    public OpportunityResponse getOpportunity(
            Long id
    ) {

        Opportunity opportunity =
                opportunityRepository.findByIdWithResponsibilities(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                    "Opportunity not found with id: "
                                    + id
                                )
                        );

        return OpportunityResponse.fromEntity(
                opportunity
        );
    }

    /**
     * Get all published opportunities.
     *
     * This is what candidates/users should normally see.
     * Fetch-joined: 1 query for opportunities + responsibilities,
     * plus a couple of batched queries for requiredSkills/qualifications
     * (via @BatchSize on the entity) instead of N queries each.
     */
    @Transactional(readOnly = true)
    public List<OpportunityResponse> getPublishedOpportunities() {

        return opportunityRepository
                .findByStatusWithResponsibilities(OpportunityStatus.PUBLISHED)
                .stream()
                .map(OpportunityResponse::fromEntity)
                .toList();
    }

    /**
     * Get all opportunities owned by the current business.
     * Fetch-joined, same reasoning as above.
     */
    @Transactional(readOnly = true)
    public List<OpportunityResponse> getMyOpportunities(
            User currentUser
    ) {

        return opportunityRepository
                .findByBusinessIdWithResponsibilities(currentUser.getId())
                .stream()
                .map(OpportunityResponse::fromEntity)
                .toList();
    }

    /**
     * Update an opportunity.
     *
     * Only its owner can update it.
     */
    @Transactional
    public OpportunityResponse updateOpportunity(
            Long id,
            OpportunityUpdateRequest request,
            User currentUser
    ) {

        Opportunity opportunity =
                opportunityRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                    "Opportunity not found with id: "
                                    + id
                                )
                        );

        verifyOwnership(opportunity, currentUser);

        opportunity.setTitle(request.getTitle());
        opportunity.setSpecialization(
                request.getSpecialization()
        );
        opportunity.setType(request.getType());
        opportunity.setExperienceLevel(
                request.getExperienceLevel()
        );
        opportunity.setWorkMode(
                request.getWorkMode()
        );
        opportunity.setLocation(request.getLocation());
        opportunity.setDescription(
                request.getDescription()
        );

        opportunity.setResponsibilities(
                request.getResponsibilities()
        );

        opportunity.setRequiredSkills(
                request.getRequiredSkills()
        );

        opportunity.setQualifications(
                request.getQualifications()
        );

        opportunity.setCompensation(
                request.getCompensation()
        );

        opportunity.setOpenings(
                request.getOpenings()
        );

        opportunity.setApplicationDeadline(
                request.getApplicationDeadline()
        );

        Opportunity updated =
                opportunityRepository.save(opportunity);

        return OpportunityResponse.fromEntity(updated);
    }

    /**
     * Publish a draft opportunity.
     */
    @Transactional
    public OpportunityResponse publishOpportunity(
            Long id,
            User currentUser
    ) {

        Opportunity opportunity =
                opportunityRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                    "Opportunity not found with id: "
                                    + id
                                )
                        );

        verifyOwnership(opportunity, currentUser);

        opportunity.setStatus(
                OpportunityStatus.PUBLISHED
        );

        Opportunity saved =
                opportunityRepository.save(opportunity);

        return OpportunityResponse.fromEntity(saved);
    }

    /**
     * Close an opportunity.
     */
    @Transactional
    public OpportunityResponse closeOpportunity(
            Long id,
            User currentUser
    ) {

        Opportunity opportunity =
                opportunityRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                    "Opportunity not found with id: "
                                    + id
                                )
                        );

        verifyOwnership(opportunity, currentUser);

        opportunity.setStatus(
                OpportunityStatus.CLOSED
        );

        Opportunity saved =
                opportunityRepository.save(opportunity);

        return OpportunityResponse.fromEntity(saved);
    }

    /**
     * Delete an opportunity.
     *
     * Only the owner can delete it.
     */
    @Transactional
    public void deleteOpportunity(
            Long id,
            User currentUser
    ) {

        Opportunity opportunity =
                opportunityRepository.findById(id)
                        .orElseThrow(() ->
                                new RuntimeException(
                                    "Opportunity not found with id: "
                                    + id
                                )
                        );

        verifyOwnership(opportunity, currentUser);

        opportunityRepository.delete(opportunity);

        log.info(
                "Opportunity {} deleted by user {}",
                id,
                currentUser.getId()
        );
    }
//    * @Transactional here is what actually fixes the LazyInitializationException.
//      * The session stays open for the ENTIRE method body — including the
//      * page.map() call below — so requiredSkills/qualifications can lazily
//      * batch-load successfully. Once this method returns, the DTOs are plain
//      * POJOs with no lazy proxies left, so it's safe to hand them to the
//      * controller and serialize them however long after that.
//      */
    @Transactional(readOnly = true)
    public Page<OpportunityResponse> search(
            FinanceSpecialization specialization,
            WorkMode workMode,
            ExperienceLevel experienceLevel,
            OpportunityType type,
            Pageable pageable) {

        Specification<Opportunity> spec = Specification
                .where(OpportunitySpecifications.isPublished())
                .and(OpportunitySpecifications.hasSpecialization(specialization))
                .and(OpportunitySpecifications.hasWorkMode(workMode))
                .and(OpportunitySpecifications.hasExperienceLevel(experienceLevel))
                .and(OpportunitySpecifications.hasType(type));

        Page<Opportunity> page = opportunityRepository.findAll(spec, pageable);
        return page.map(opportunityMapper::toResponse);
    }

    /**
     * Security check.
     *
     * A user must never be able to modify another
     * business's opportunity.
     */
    private void verifyOwnership(
            Opportunity opportunity,
            User currentUser
    ) {

        if (!opportunity.getBusiness().getId()
                .equals(currentUser.getId())) {

            throw new AccessDeniedException(
                    "You do not have permission to modify "
                    + "this opportunity"
            );
        }
    }
}