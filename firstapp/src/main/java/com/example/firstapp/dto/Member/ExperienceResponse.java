package com.example.firstapp.dto.Member;

import com.example.firstapp.entity.MemberExperience;

import java.time.LocalDate;


public record ExperienceResponse(

        Long id,

        String jobTitle,

        String companyName,

       


        LocalDate startDate,

        LocalDate endDate,

        String description


) {

    public static ExperienceResponse fromEntity(
            MemberExperience experience
    ) {

        if (experience == null) {
            return null;
        }

        return new ExperienceResponse(
                experience.getId(),
                experience.getJobTitle(),
                experience.getCompanyName(),
                experience.getStartDate(),
                experience.getEndDate(),
                experience.getDescription()
               
        );
    }
}
