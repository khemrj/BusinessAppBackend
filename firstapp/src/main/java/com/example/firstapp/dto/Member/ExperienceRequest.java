package com.example.firstapp.dto.Member;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


// dto/request/ExperienceRequest.java

public record ExperienceRequest(
    @NotBlank @Size(max = 150) String companyName,
    @NotBlank @Size(max = 150) String title,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    @Size(max = 1000) String description
) {}