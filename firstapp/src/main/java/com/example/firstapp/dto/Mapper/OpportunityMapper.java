package com.example.firstapp.dto.Mapper;

import com.example.firstapp.dto.OpportunityResponse;
import com.example.firstapp.entity.Opportunity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class OpportunityMapper {

    public OpportunityResponse toResponse(Opportunity opportunity) {
        if (opportunity == null) {
            return null;
        }

        return OpportunityResponse.builder()
                .id(opportunity.getId())
                .businessId(opportunity.getBusiness().getId())
                .businessName(opportunity.getBusiness().getCompanyName())
                .title(opportunity.getTitle())
                .specialization(opportunity.getSpecialization().name())
                .type(opportunity.getType().name())
                .experienceLevel(opportunity.getExperienceLevel().name())
                .workMode(opportunity.getWorkMode().name())
                .location(opportunity.getLocation())
                .description(opportunity.getDescription())
                .responsibilities(new ArrayList<>(opportunity.getResponsibilities()))
                .requiredSkills(new ArrayList<>(opportunity.getRequiredSkills()))
                .qualifications(new ArrayList<>(opportunity.getQualifications()))
                .compensationMin(opportunity.getCompensation().getMin())
                .compensationMax(opportunity.getCompensation().getMax())
                .compensationCurrency(opportunity.getCompensation().getCurrency())
                .compensationDisclosed(opportunity.getCompensation().isDisclosed())
                .openings(opportunity.getOpenings())
                .status(opportunity.getStatus().name())
                .postedAt(opportunity.getPostedAt())
                .applicationDeadline(opportunity.getApplicationDeadline())
                .createdAt(opportunity.getCreatedAt())
                .updatedAt(opportunity.getUpdatedAt())
                .build();
    }
}