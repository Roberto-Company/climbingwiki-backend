package com.roberto.climbingwiki.dtos;

import com.roberto.climbingwiki.model.entities.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        String fullName,
        String bio,
        String avatarUrl,
        User.Role role) {}
