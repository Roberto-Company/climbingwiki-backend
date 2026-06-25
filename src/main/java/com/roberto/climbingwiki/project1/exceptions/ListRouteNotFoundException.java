package com.roberto.climbingwiki.project1.exceptions;

public class ListRouteNotFoundException extends RuntimeException {

    public ListRouteNotFoundException(Long listId, Long routeId) {
        super("List route not found: listId=" + listId + ", routeId=" + routeId);
    }
}
