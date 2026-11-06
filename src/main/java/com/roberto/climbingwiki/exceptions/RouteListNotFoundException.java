package com.roberto.climbingwiki.exceptions;

public class RouteListNotFoundException extends RuntimeException {

    public RouteListNotFoundException(Long id) {
        super("Route list not found: id=" + id);
    }
}
