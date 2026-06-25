package com.roberto.climbingwiki.project1.exceptions;

public class RouteListNotFoundException extends RuntimeException {

    public RouteListNotFoundException(Long id) {
        super("Route list not found: id=" + id);
    }
}
