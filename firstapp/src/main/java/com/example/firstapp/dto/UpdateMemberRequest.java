package com.example.firstapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMemberRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 30)
    @Pattern(
        regexp = "^\\+?[0-9\\s().-]{7,30}$",
        message = "Invalid phone number"
    )
    private String phone;

    @Size(max = 255)
    private String headline;

    @Size(max = 5000)
    private String bio;

    @Size(max = 100)
    private String industry;

    @Email
    @Size(max = 150)
    private String contactEmail;

    @Size(max = 500)
    private String websiteUrl;

    @Size(max = 500)
    private String linkedinUrl;

    @Size(max = 500)
    private String githubUrl;

    @Valid
    private AddressRequest location;
}