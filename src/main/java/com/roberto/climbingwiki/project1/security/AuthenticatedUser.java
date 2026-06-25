package com.roberto.climbingwiki.project1.security;

import com.roberto.climbingwiki.project1.model.entities.User;

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
}
