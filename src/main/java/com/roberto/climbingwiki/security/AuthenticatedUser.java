package com.roberto.climbingwiki.security;

import com.roberto.climbingwiki.model.entities.User;

public record AuthenticatedUser(Long id, String username, User.Role role) {

    public boolean isModeratorOrAdmin() {
        return role == User.Role.MODERATOR || role == User.Role.ADMIN;
    }

    public boolean isAdmin() {
        return role == User.Role.ADMIN;
    }

    public boolean isSelfOrModeratorOrAdmin(Long ownerId) {
        return id.equals(ownerId) || isModeratorOrAdmin();
    }

    /** Private contact data (e.g. email) is visible only to its owner and to admins, not moderators. */
    public boolean canSeePrivateDataOf(Long ownerId) {
        return id.equals(ownerId) || isAdmin();
    }
}
