package com.roberto.climbingwiki.dtos;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(@NotBlank String token) {}
