package com.example.firstapp.dto.Member;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


// dto/request/EducationRequest.java

public record EducationRequest(
    @NotBlank @Size(max = 150) String institution,
    @NotBlank @Size(max = 150) String degree,
    @NotNull LocalDate startDate,
    LocalDate endDate,
    @Size(max = 1000) String description,
    @Size(max = 150) String fieldOfStudy
) {

   
}
