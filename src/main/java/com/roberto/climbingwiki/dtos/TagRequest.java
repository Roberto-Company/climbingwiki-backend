package com.roberto.climbingwiki.dtos;

import jakarta.validation.constraints.NotBlank;

public record TagRequest(@NotBlank String name) {}
