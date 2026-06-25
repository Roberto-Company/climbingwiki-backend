package com.roberto.climbingwiki.project1.exceptions;

public class FavoriteNotFoundException extends RuntimeException {

    public FavoriteNotFoundException(Long userId, Long routeId) {
        super("Favorite not found: userId=" + userId + ", routeId=" + routeId);
    }
}
