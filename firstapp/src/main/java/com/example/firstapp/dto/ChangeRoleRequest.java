package com.example.firstapp.dto;

import com.example.firstapp.enums.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull Role role) {}