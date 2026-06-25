package com.roberto.climbingwiki.project1.dtos;

import jakarta.validation.constraints.NotBlank;

public record ZoneRequest(Long parentZoneId, @NotBlank String name, String description) {}
