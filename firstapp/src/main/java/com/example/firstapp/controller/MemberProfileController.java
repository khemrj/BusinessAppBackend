package com.example.firstapp.controller;


import com.example.firstapp.dto.Member.CertificationRequest;
import com.example.firstapp.dto.Member.CertificationResponse;
import com.example.firstapp.dto.Member.EducationRequest;
import com.example.firstapp.dto.Member.EducationResponse;
import com.example.firstapp.dto.Member.ExperienceRequest;
import com.example.firstapp.dto.Member.ExperienceResponse;
import com.example.firstapp.dto.Member.MemberCreateRequest;
import com.example.firstapp.dto.Member.MemberProfileResponse;
import com.example.firstapp.entity.User;
import com.example.firstapp.service.Member.MemberCertificationService;
import com.example.firstapp.service.Member.MemberEducationService;
import com.example.firstapp.service.Member.MemberExperienceService;
import com.example.firstapp.service.Member.MemberService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@Slf4j 
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
@Validated
public class MemberProfileController {

    private final MemberService memberService;
    private final MemberExperienceService experienceService;
    private final MemberEducationService educationService;
    private final MemberCertificationService certificationService;


    /**
     * Creates a profile for the currently authenticated user.
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<MemberProfileResponse> create(
            @Valid @RequestBody MemberCreateRequest request,
            @AuthenticationPrincipal User user
    ) {
        log.info("Creating member profile for user:\n\n\n\n " + user.getUsername());
        MemberProfileResponse response =
                memberService.createMember(user, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    /**
     * Adds an experience to a member profile.
     */
    @PostMapping("/{id}/experiences")
    public ResponseEntity<ExperienceResponse> addExperience(
            @PathVariable
            @Positive(message = "Member ID must be positive")
            Long id,

            @Valid @RequestBody
            ExperienceRequest request
    ) {

        ExperienceResponse response =
                experienceService.add(id, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    /**
     * Adds an education to a member profile.
     */
    @PostMapping("/{id}/educations")
    public ResponseEntity<EducationResponse> addEducation(
            @PathVariable
            @Positive(message = "Member ID must be positive")
            Long id,

            @Valid @RequestBody
            EducationRequest request
    ) {

        EducationResponse response =
                educationService.add(id, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    /**
     * Adds a certification to a member profile.
     */
    @PostMapping("/{id}/certifications")
    public ResponseEntity<CertificationResponse> addCertification(
            @PathVariable
            @Positive(message = "Member ID must be positive")
            Long id,

            @Valid @RequestBody
            CertificationRequest request
    ) {

        CertificationResponse response =
                certificationService.add(id, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    //get all educations details for the current user
   @GetMapping("/me/educations")
public ResponseEntity<List<EducationResponse>> getMyEducations(
        @AuthenticationPrincipal User user
) {
    Long memberId = memberService.getMemberIdByUserId(user.getId());
    return ResponseEntity.ok(educationService.getEducationByMemberId(memberId));
}
    
    }
    
