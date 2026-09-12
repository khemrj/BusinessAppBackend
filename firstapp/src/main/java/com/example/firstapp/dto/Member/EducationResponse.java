package com.example.firstapp.dto.Member;

import java.time.LocalDate;

public record EducationResponse(
        Long id,
        String institution,
        String degree,
        String fieldOfStudy,
        LocalDate startDate,
        LocalDate endDate
       
) {

    public static EducationResponse fromEntity(
            com.example.firstapp.entity.MemberEducation education
    ) {

        if (education == null) {
            return null;
        }

        return new EducationResponse(
                education.getId(),
                education.getInstitutionName(),
                education.getDegree(),
                education.getFieldOfStudy(),
                education.getStartDate(),
                education.getEndDate()
        );
    }
}