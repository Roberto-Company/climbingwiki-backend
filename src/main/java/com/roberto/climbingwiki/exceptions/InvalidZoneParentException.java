package com.roberto.climbingwiki.exceptions;

public class InvalidZoneParentException extends RuntimeException {

    public InvalidZoneParentException(Long id) {
        super("Zone cannot be its own parent: id=" + id);
    }
}
