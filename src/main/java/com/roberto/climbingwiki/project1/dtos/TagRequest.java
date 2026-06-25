package com.roberto.climbingwiki.project1.dtos;

import jakarta.validation.constraints.NotBlank;

public record TagRequest(@NotBlank String name) {}
