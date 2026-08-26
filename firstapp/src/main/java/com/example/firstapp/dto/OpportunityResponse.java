package com.example.firstapp.dto;

import com.example.firstapp.entity.Opportunity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class OpportunityResponse {

    private Long id;

    private Long businessId;

    private String businessName;

    private String title;

    private String specialization;

    private String type;

    private String experienceLevel;

    private String workMode;

    private String location;

    private String description;

    private List<String> responsibilities;

    private List<String> requiredSkills;

    private List<String> qualifications;

    private Double compensationMin;

    private Double compensationMax;

    private String compensationCurrency;

    private boolean compensationDisclosed;

    private Integer openings;

    private String status;

    private LocalDateTime postedAt;

    private LocalDateTime applicationDeadline;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static OpportunityResponse fromEntity(
            Opportunity opportunity
    ) {

        return OpportunityResponse.builder()
                .id(opportunity.getId())
                .businessId(opportunity.getBusiness().getId())
                .businessName(opportunity.getBusiness().getCompanyName())
                .title(opportunity.getTitle())
                .specialization(
                        opportunity.getSpecialization().name()
                )
                .type(
                        opportunity.getType().name()
                )
                .experienceLevel(
                        opportunity.getExperienceLevel().name()
                )
                .workMode(
                        opportunity.getWorkMode().name()
                )
                .location(opportunity.getLocation())
                .description(opportunity.getDescription())

                // IMPORTANT: wrap in new ArrayList<>() to force Hibernate
                // to actually initialize these collections HERE, while
                // the session is still open (we're inside a @Transactional
                // service method). This also detaches them from the
                // Hibernate proxy so Jackson can serialize safely later
                // regardless of session state.
                .responsibilities(
                        new ArrayList<>(opportunity.getResponsibilities())
                )
                .requiredSkills(
                        new ArrayList<>(opportunity.getRequiredSkills())
                )
                .qualifications(
                        new ArrayList<>(opportunity.getQualifications())
                )

                .compensationMin(
                        opportunity.getCompensation().getMin()
                )
                .compensationMax(
                        opportunity.getCompensation().getMax()
                )
                .compensationCurrency(
                        opportunity.getCompensation().getCurrency()
                )
                .compensationDisclosed(
                        opportunity.getCompensation().isDisclosed()
                )
                .openings(opportunity.getOpenings())
                .status(opportunity.getStatus().name())
                .postedAt(opportunity.getPostedAt())
                .applicationDeadline(
                        opportunity.getApplicationDeadline()
                )
                .createdAt(opportunity.getCreatedAt())
                .updatedAt(opportunity.getUpdatedAt())
                .build();
    }
}