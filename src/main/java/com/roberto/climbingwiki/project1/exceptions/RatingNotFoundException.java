package com.roberto.climbingwiki.project1.exceptions;

public class RatingNotFoundException extends RuntimeException {

    public RatingNotFoundException(Long userId, Long routeId) {
        super("Rating not found: userId=" + userId + ", routeId=" + routeId);
    }
}
