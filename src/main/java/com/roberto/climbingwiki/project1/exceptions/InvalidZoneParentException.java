package com.roberto.climbingwiki.project1.exceptions;

public class InvalidZoneParentException extends RuntimeException {

    public InvalidZoneParentException(Long id) {
        super("Zone cannot be its own parent: id=" + id);
    }
}
