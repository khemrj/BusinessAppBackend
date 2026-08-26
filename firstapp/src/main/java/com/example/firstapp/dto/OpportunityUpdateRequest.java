package com.example.firstapp.dto;

import com.example.firstapp.entity.CompensationRange;
import com.example.firstapp.enums.ExperienceLevel;
import com.example.firstapp.enums.FinanceSpecialization;
import com.example.firstapp.enums.OpportunityType;
import com.example.firstapp.enums.WorkMode;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OpportunityUpdateRequest {

    @NotBlank(message = "Opportunity title is required")
    private String title;

    @NotNull(message = "Finance specialization is required")
    private FinanceSpecialization specialization;

    @NotNull(message = "Opportunity type is required")
    private OpportunityType type;

    @NotNull(message = "Experience level is required")
    private ExperienceLevel experienceLevel;

    @NotNull(message = "Work mode is required")
    private WorkMode workMode;

    @NotBlank(message = "Location is required")
    private String location;

    @NotBlank(message = "Description is required")
    private String description;

    @NotEmpty(message = "At least one responsibility is required")
    private List<String> responsibilities;

    @NotEmpty(message = "At least one required skill is required")
    private List<String> requiredSkills;

    @NotEmpty(message = "At least one qualification is required")
    private List<String> qualifications;

    @NotNull(message = "Compensation information is required")
    @Valid
    private CompensationRange compensation;

    @NotNull(message = "Number of openings is required")
    @Min(value = 1, message = "There must be at least one opening")
    private Integer openings;

    private LocalDateTime applicationDeadline;
}