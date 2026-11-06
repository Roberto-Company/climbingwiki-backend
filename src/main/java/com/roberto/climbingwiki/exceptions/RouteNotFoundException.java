package com.roberto.climbingwiki.exceptions;

public class RouteNotFoundException extends RuntimeException {

    public RouteNotFoundException(Long id) {
        super("Route not found: id=" + id);
    }
}
