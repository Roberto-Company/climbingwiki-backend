package com.roberto.climbingwiki.dtos;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
