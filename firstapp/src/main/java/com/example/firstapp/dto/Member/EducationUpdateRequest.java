package com.example.firstapp.dto.Member;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record EducationUpdateRequest(

    @Size(max = 200)
    String institution,

    @Size(max = 150)
    String degree,

    @Size(max = 150)
    String fieldOfStudy,

    LocalDate startDate,

    LocalDate endDate
) {}
