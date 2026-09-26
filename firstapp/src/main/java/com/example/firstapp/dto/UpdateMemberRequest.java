package com.example.firstapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

public record UpdateMemberRequest (

    @Size(max = 100)
    String firstName,

    @Size(max = 100)
     String lastName,

    @Size(max = 30)
    @Pattern(
        regexp = "^\\+?[0-9\\s().-]{7,30}$",
        message = "Invalid phone number"
    )
     String phone,

    @Size(max = 255)
     String headline,

    @Size(max = 5000)
    String bio,

    @Size(max = 100)
     String industry,

    @Email
    @Size(max = 150)
     String contactEmail,

    @Size(max = 500)
     String websiteUrl,

    @Size(max = 500)
     String linkedinUrl,

    @Size(max = 500)
     String githubUrl,

    @Valid
     AddressRequest location
)
{}