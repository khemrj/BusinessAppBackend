// dto/request/MemberCreateRequest.java

package com.example.firstapp.dto.Member;

import com.example.firstapp.dto.AddressRequest;
import com.example.firstapp.enums.FinanceSpecialization;
import com.example.firstapp.enums.ProfileVisibility;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MemberCreateRequest(

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Size(max = 255, message = "Headline must not exceed 255 characters")
        String headline,

        @Size(max = 2000, message = "Bio must not exceed 2000 characters")
        String bio,

        FinanceSpecialization specialization,

        @Email(message = "Enter a valid email address")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String contactEmail,

        @Pattern(
                regexp = "^\\+?[0-9]{7,15}$",
                message = "Enter a valid phone number"
        )
        String phone,

        @Min(value = 0, message = "Years of experience cannot be negative")
        @Max(value = 60, message = "Years of experience cannot exceed 60")
        Integer yearsOfExperience,

        @Pattern(
                regexp = "^https?://(www\\.)?github\\.com/[a-zA-Z0-9_-]+/?$",
                message = "Enter a valid GitHub profile URL"
        )
        String githubUrl,

        @Pattern(
                regexp = "^https?://(www\\.)?linkedin\\.com/in/[a-zA-Z0-9-]+/?$",
                message = "Enter a valid LinkedIn profile URL"
        )
        String linkedinUrl,

        @Size(max = 500, message = "Website URL must not exceed 500 characters")
        String websiteUrl,

        ProfileVisibility profileVisibility,

        Boolean openToWork,

        @Valid
        AddressRequest location

) {
}