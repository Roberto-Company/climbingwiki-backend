package com.roberto.climbingwiki.project1.dtos;

import com.roberto.climbingwiki.project1.model.entities.User;
import com.roberto.climbingwiki.project1.security.AuthenticatedUser;
import java.time.LocalDateTime;

/** {@code email} is null unless the viewer is the user themselves or an admin. */
public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String bio,
        String avatarUrl,
        User.Role role,
        LocalDateTime createdAt) {

    /** Full view, including email. Only for responses that go back to the user's own account. */
    public static UserResponse from(User user) {
        return build(user, true);
    }

    /** View as seen by {@code viewer}, who may be null for anonymous requests. */
    public static UserResponse forViewer(User user, AuthenticatedUser viewer) {
        boolean includeEmail = viewer != null && viewer.canSeePrivateDataOf(user.getId());
        return build(user, includeEmail);
    }

    private static UserResponse build(User user, boolean includeEmail) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                includeEmail ? user.getEmail() : null,
                user.getFullName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getCreatedAt());
    }
}
