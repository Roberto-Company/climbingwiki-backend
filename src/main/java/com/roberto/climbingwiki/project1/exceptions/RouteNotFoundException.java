package com.roberto.climbingwiki.project1.exceptions;

public class RouteNotFoundException extends RuntimeException {

    public RouteNotFoundException(Long id) {
        super("Route not found: id=" + id);
    }
}
