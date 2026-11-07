package com.roberto.climbingwiki.dtos;

import jakarta.validation.constraints.Size;

/** The public profile fields a user edits from their account page. Blank values clear the field. */
public record ProfileRequest(@Size(max = 150) String fullName, @Size(max = 1000) String bio) {}
