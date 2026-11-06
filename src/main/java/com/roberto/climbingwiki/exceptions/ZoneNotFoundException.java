package com.roberto.climbingwiki.exceptions;

public class ZoneNotFoundException extends RuntimeException {

    public ZoneNotFoundException(Long id) {
        super("Zone not found: id=" + id);
    }
}
