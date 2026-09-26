package com.example.firstapp.dto;

import com.example.firstapp.enums.PostVisibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostRequest(
        @NotBlank @Size(max = 5000) String content,
        String mediaUrl,
        PostVisibility visibility
) {}
