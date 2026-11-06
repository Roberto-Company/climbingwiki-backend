package com.roberto.climbingwiki.exceptions;

public class RatingNotFoundException extends RuntimeException {

    public RatingNotFoundException(Long userId, Long routeId) {
        super("Rating not found: userId=" + userId + ", routeId=" + routeId);
    }
}
