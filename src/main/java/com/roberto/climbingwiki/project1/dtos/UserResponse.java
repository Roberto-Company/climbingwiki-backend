package com.roberto.climbingwiki.project1.dtos;

import com.roberto.climbingwiki.project1.model.entities.User;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String bio,
        String avatarUrl,
        User.Role role,
        LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getCreatedAt());
    }
}
