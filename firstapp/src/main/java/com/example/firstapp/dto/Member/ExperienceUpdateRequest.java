package com.example.firstapp.dto.Member;


import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ExperienceUpdateRequest(

        @Size(
                max = 200,
                message = "Job title must not exceed 200 characters"
        )
        String jobTitle,

        @Size(
                max = 200,
                message = "Company name must not exceed 200 characters"
        )
        String companyName,

        @Size(
                max = 100,
                message = "Employment type must not exceed 100 characters"
        )
        String employmentType,

        @Size(
                max = 200,
                message = "Location must not exceed 200 characters"
        )
        String location,

        LocalDate startDate,

        LocalDate endDate,

        @Size(
                max = 5000,
                message = "Description must not exceed 5000 characters"
        )
        String description

) {
}
