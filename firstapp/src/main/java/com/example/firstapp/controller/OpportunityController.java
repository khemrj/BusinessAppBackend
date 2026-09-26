package com.example.firstapp.controller;

import com.example.firstapp.dto.ApiResponse;
import com.example.firstapp.dto.OpportunityCreateRequest;
import com.example.firstapp.dto.OpportunityResponse;
import com.example.firstapp.dto.OpportunityUpdateRequest;
import com.example.firstapp.dto.PageResponse;
import com.example.firstapp.dto.Mapper.OpportunityMapper;
import com.example.firstapp.entity.User;
import com.example.firstapp.enums.ExperienceLevel;
import com.example.firstapp.enums.FinanceSpecialization;
import com.example.firstapp.enums.OpportunityType;
import com.example.firstapp.enums.WorkMode;
import com.example.firstapp.service.OpportunityService;
import org.springframework.data.domain.Sort;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Pageable;  // CORRECT
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opportunities")
@RequiredArgsConstructor
@Slf4j
public class OpportunityController {

    private final OpportunityService opportunityService;
    private final OpportunityMapper opportunityMapper;
    /**
     * POST /api/v1/opportunities
     *
     * Creates a new opportunity.
     *
     * Requires authentication.
     *
     * Business ID is obtained from JWT User.
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OpportunityResponse>>
    createOpportunity(

            @RequestBody @Valid
            OpportunityCreateRequest request,

            @AuthenticationPrincipal
            User currentUser
    ) {

        log.info(
                "Create opportunity request from user: {}",
                currentUser.getId()
        );

        OpportunityResponse response =
                opportunityService.createOpportunity(
                        request,
                        currentUser
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                    ApiResponse.success(
                        "Opportunity created successfully"
                    )
                );
    }

    /**
     * GET /api/v1/opportunities/{id}
     *
     * Get one opportunity.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OpportunityResponse>>
    getOpportunity(
            @PathVariable Long id
    ) {

        OpportunityResponse response =
                opportunityService.getOpportunity(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                    "Opportunity fetched successfully",
                    response
                )
        );
    }

    /**
     * GET /api/v1/opportunities
     *
     * Get all PUBLISHED opportunities.
     *
     * This endpoint is intentionally public.
     * Candidates do not need to be authenticated just8
     * to browse opportunities.
     */
   
    /// pageable opportunities using search and filter
@GetMapping
public ResponseEntity<ApiResponse<PageResponse<OpportunityResponse>>> getOpportunities(
        @RequestParam(required = false) FinanceSpecialization specialization,
        @RequestParam(required = false) WorkMode workMode,
        @RequestParam(required = false) ExperienceLevel experienceLevel,
        @RequestParam(required = false) OpportunityType type,
        @PageableDefault(size = 20, sort = "postedAt", direction = Sort.Direction.DESC) Pageable pageable) {

    Page<OpportunityResponse> mapped =
            opportunityService.search(specialization, workMode, experienceLevel, type, pageable);

    return ResponseEntity.ok(
        ApiResponse.success("Opportunities fetched successfully", PageResponse.from(mapped))
    );
}

    /**
     * GET /api/v1/opportunities/m
     *
     * Get opportunities belonging to the authenticated user.
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<OpportunityResponse>>>
    getMyOpportunities(
            @AuthenticationPrincipal User currentUser
    ) {

        List<OpportunityResponse> opportunities =
                opportunityService.getMyOpportunities(
                        currentUser
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                    "Your opportunities fetched successfully",
                    opportunities
                )
        );
    }

    /**
     * PUT /api/v1/opportunities/{id}
     *
     * Update an opportunity.
     *
     * Only the owner can update it.
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OpportunityResponse>>
    updateOpportunity(

            @PathVariable Long id,

            @RequestBody @Valid
            OpportunityUpdateRequest request,

            @AuthenticationPrincipal
            User currentUser
    ) {

        OpportunityResponse response =
                opportunityService.updateOpportunity(
                        id,
                        request,
                        currentUser
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                    "Opportunity updated successfully",
                    response
                )
        );
    }

    /**
     * PATCH /api/v1/opportunities/{id}/publish
     *
     * Publish a draft.
     */
    @PatchMapping("/{id}/publish")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OpportunityResponse>>
    publishOpportunity(

            @PathVariable Long id,

            @AuthenticationPrincipal
            User currentUser
    ) {

        OpportunityResponse response =
                opportunityService.publishOpportunity(
                        id,
                        currentUser
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                    "Opportunity published successfully",
                    response
                )
        );
    }

    /**
     * PATCH /api/v1/opportunities/{id}/close
     *
     * Close an opportunity.
     */
    @PatchMapping("/{id}/close")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<OpportunityResponse>>
    closeOpportunity(

            @PathVariable Long id,

            @AuthenticationPrincipal
            User currentUser
    ) {

        OpportunityResponse response =
                opportunityService.closeOpportunity(
                        id,
                        currentUser
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                    "Opportunity closed successfully",
                    response
                )
        );
    }

    /**
     * DELETE /api/v1/opportunities/{id}
     *
     * Delete an opportunity.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>>
    deleteOpportunity(

            @PathVariable Long id,

            @AuthenticationPrincipal
            User currentUser
    ) {

        opportunityService.deleteOpportunity(
                id,
                currentUser
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                    "Opportunity deleted successfully"
                )
        );
    }
}