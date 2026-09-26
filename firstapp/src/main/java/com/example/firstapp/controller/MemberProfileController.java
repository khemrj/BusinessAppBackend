package com.example.firstapp.controller;

import com.example.firstapp.dto.ApiResponse;
import com.example.firstapp.dto.UpdateMemberRequest;
import com.example.firstapp.dto.Member.CertificationRequest;
import com.example.firstapp.dto.Member.CertificationResponse;
import com.example.firstapp.dto.Member.EducationRequest;
import com.example.firstapp.dto.Member.EducationResponse;
import com.example.firstapp.dto.Member.EducationUpdateRequest;
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
public ResponseEntity<ApiResponse<MemberProfileResponse>> create(
        @Valid @RequestBody MemberCreateRequest request,
        @AuthenticationPrincipal User user) {

    log.info("Creating member profile for user: {}", user.getUsername());

    MemberProfileResponse response =
            memberService.createMember(user, request);

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(
                    "Profile created",
                    response
            ));
}

        @PreAuthorize ("isAuthenticated()")
        @PatchMapping("/me/profile")
        public ResponseEntity<MemberProfileResponse> updateMyProfile(
                @Valid @RequestBody UpdateMemberRequest request,
                @AuthenticationPrincipal User user) {
                MemberProfileResponse response = memberService.updateMyProfile(user.getId(), request);
                return ResponseEntity.ok(response);
        }

        @PatchMapping("/me/educations/{educationId}")
public ResponseEntity<EducationResponse> updateMyEducation(
        @PathVariable Long educationId,
        @Valid @RequestBody EducationUpdateRequest request,
        @AuthenticationPrincipal User user
) {

    EducationResponse response =
            educationService.updateMyEducation(
                    user.getId(),
                    educationId,
                    request
            );

    return ResponseEntity.ok(response);
}

        @PatchMapping("/me/certifications/{certificationId}")
        public ResponseEntity<CertificationResponse> updateMyCertification(
                        @PathVariable Long certificationId,
                        @Valid @RequestBody CertificationRequest request,
                        @AuthenticationPrincipal User user) {

                CertificationResponse response = certificationService.update(
                                user.getId(),
                                certificationId,
                                request);

                return ResponseEntity.ok(response);
        }

        @PatchMapping("/me/experiences/{experienceId}")
        public ResponseEntity<ExperienceResponse> updateMyExperience(
                        @PathVariable Long experienceId,
                        @Valid @RequestBody ExperienceRequest request,
                        @AuthenticationPrincipal User user) {

                ExperienceResponse response = experienceService.updateMyExperience(
                                user.getId(),
                                experienceId,
                                request);

                return ResponseEntity.ok(response);
        }

        /**
         * Adds an experience to a member profile.
         */
        @PostMapping("/me/experiences")
        public ResponseEntity<ExperienceResponse> addMyExperience(
                        @Valid @RequestBody ExperienceRequest request,
                        @AuthenticationPrincipal User user) {

                ExperienceResponse response = experienceService.addExperience(
                                user.getId(),
                                request);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response);
        }

        /**
         * Adds an education to a member profile.
         */
        @PostMapping("/me/educations")
public ResponseEntity<EducationResponse> addMyEducation(
        @Valid @RequestBody EducationRequest request,
        @AuthenticationPrincipal User user
) {

    EducationResponse response =
            educationService.addEducation(
                    user.getId(),
                    request
            );

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
}

        /**
         * Adds a certification to a member profile.
         */
        @PostMapping("/{id}/certifications")
        public ResponseEntity<CertificationResponse> addCertification(
                        @PathVariable @Positive(message = "Member ID must be positive") Long id,

                        @Valid @RequestBody CertificationRequest request) {

                CertificationResponse response = certificationService.add(id, request);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response);
        }

        @PostMapping("/me/certifications")
        public ResponseEntity<CertificationResponse> addMyCertification(
                        @Valid @RequestBody CertificationRequest request,
                        @AuthenticationPrincipal User user) {

                CertificationResponse response = certificationService.add(
                                user.getId(),
                                request);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response);
        }

        // get all educations details for the current user
        @GetMapping("/me/educations")
public ResponseEntity<List<EducationResponse>> getMyEducations(
        @AuthenticationPrincipal User user
) {

    return ResponseEntity.ok(
            educationService.getMyEducations(
                    user.getId()
            )
    );
}

        @GetMapping("/me/experiences")
        public ResponseEntity<List<ExperienceResponse>> getMyExperiences(
                        @AuthenticationPrincipal User user) {

                return ResponseEntity.ok(
                                experienceService.getMyExperiences(
                                                user.getId()));
        }

        @GetMapping("/me/certifications")
        public ResponseEntity<List<CertificationResponse>> getMyCertifications(
                        @AuthenticationPrincipal User user) {

                return ResponseEntity.ok(
                                certificationService.getMyCertifications(
                                                user.getId()));
        }

        @DeleteMapping("/me/certifications/{certificationId}")
        public ResponseEntity<Void> deleteMyCertification(
                        @PathVariable Long certificationId,
                        @AuthenticationPrincipal User user) {

                certificationService.delete(
                                user.getId(),
                                certificationId);

                return ResponseEntity.noContent().build();
        }

        @DeleteMapping("/me/experiences/{experienceId}")
        public ResponseEntity<Void> deleteMyExperience(
                        @PathVariable Long experienceId,
                        @AuthenticationPrincipal User user) {

                experienceService.deleteMyExperience(
                                user.getId(),
                                experienceId);

                return ResponseEntity.noContent().build();
        }
        @DeleteMapping("/me/educations/{educationId}")
public ResponseEntity<Void> deleteMyEducation(
        @PathVariable Long educationId,
        @AuthenticationPrincipal User user
) {

    educationService.deleteMyEducation(
            user.getId(),
            educationId
    );

    return ResponseEntity.noContent().build();
}
                           @GetMapping("/{slug}")
    public ResponseEntity<MemberProfileResponse> getProfile(
            @PathVariable String slug
    ) {
        MemberProfileResponse response =
                memberService.getProfileBySlug(slug);

        return ResponseEntity.ok(response);
    }

}
