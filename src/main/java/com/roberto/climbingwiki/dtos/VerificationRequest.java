package com.roberto.climbingwiki.dtos;

import jakarta.validation.constraints.NotNull;

/** Body of {@code PUT /api/{catalog-type}/{id}/verification}; moderators and admins only. */
public record VerificationRequest(@NotNull Boolean verified) {}
